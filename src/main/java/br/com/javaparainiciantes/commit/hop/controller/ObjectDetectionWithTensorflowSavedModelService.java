/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package br.com.javaparainiciantes.commit.hop.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.google.gson.annotations.SerializedName;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ObjectDetectionWithTensorflowSavedModelService {
	
	public Path predict(ObjectDetectionDto dto, File image ) throws IOException, ModelNotFoundException, MalformedModelException, TranslateException {
        Path imageFile = Paths.get(image.getAbsolutePath());
        Image img = ImageFactory.getInstance().fromFile(imageFile);

        String modelUrl = dto.getModelUrl();
        DataType dataType = DataType.valueOf(dto.getInputDataType());
        int width = dto.getInputWidth();
        int heigth = dto.getInputHeigth();

        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .optApplication(Application.CV.OBJECT_DETECTION)
                        .setTypes(Image.class, DetectedObjects.class)
                        .optModelUrls(modelUrl)
                        // saved_model.pb file is in the subfolder of the model archive file
                        .optModelName("saved_model")
                        .optTranslator(new MyTranslator(dataType, width, heigth))
                        .optEngine("TensorFlow")
                        .optProgress(new ProgressBar())
                        .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
                Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detection = predictor.predict(img);
            return saveBoundingBoxImage(img, detection, imageFile);
        }		
	}

    private Path saveBoundingBoxImage(Image img, DetectedObjects detection, Path imageFile)
            throws IOException {
        Path outputDir = imageFile.getParent();
        Files.createDirectories(outputDir);

        img.drawBoundingBoxes(detection);

        Path imagePath = outputDir.resolve("detected-" + imageFile.getFileName().toString());
        // OpenJDK can't save jpg with alpha channel
        img.save(Files.newOutputStream(imagePath), "png");
        log.info("Detected objects image has been saved in: {}", imagePath);
        return imagePath;
    }

    

    private static final class Item {
        int id;

        @SerializedName("display_name")
        String displayName;
    }

    private static final class MyTranslator
            implements NoBatchifyTranslator<Image, DetectedObjects> {

        private Map<Integer, String> classes;
        private int maxBoxes;
        private float threshold;
        private DataType inputDataType;
		private int inputHeight;
		private int inputWidth;

        MyTranslator() {
            maxBoxes = 20;
            threshold = 0.7f;
            inputDataType = DataType.UINT8;
            inputHeight = 224;
            inputWidth = 224;
        }
        
        MyTranslator(DataType input, int width, int heigth){
        	this();
        	inputDataType = input;
        	inputWidth = width;
        	inputHeight = heigth;
        }

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            // input to tf object-detection models is a list of tensors, hence NDList
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
			array = NDImageUtils.resize(array, inputWidth, inputHeight);
            // tf object-detection models expect 8 bit unsigned integer tensor
            array = array.toType(inputDataType, true);
            array = array.expandDims(0); // tf object-detection models expect a 4 dimensional input
            return new NDList(array);
        }

        /** {@inheritDoc} */
        @Override
        public void prepare(TranslatorContext ctx) throws IOException {
            if (classes == null) {
                classes = loadSynset();
            }
        }
        
        static Map<Integer, String> loadSynset() throws IOException {
            URL synsetUrl =
                    new URL(
                            "https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/mscoco_label_map.pbtxt");
            Map<Integer, String> map = new ConcurrentHashMap<>();
            int maxId = 0;
            try (InputStream is = new BufferedInputStream(synsetUrl.openStream());
                    Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("item ");
                while (scanner.hasNext()) {
                    String content = scanner.next();
                    content = content.replaceAll("(\"|\\d)\\n\\s", "$1,");
                    Item item = JsonUtils.GSON.fromJson(content, Item.class);
                    map.put(item.id, item.displayName);
                    if (item.id > maxId) {
                        maxId = item.id;
                    }
                }
            }
            return map;
        }

        /** {@inheritDoc} */
        @Override
        public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
            // output of tf object-detection models is a list of tensors, hence NDList in djl
            // output NDArray order in the list are not guaranteed

            int[] classIds = null;
            float[] probabilities = null;
            NDArray boundingBoxes = null;
            for (NDArray array : list) {
                if ("detection_boxes".equals(array.getName())) {
                    boundingBoxes = array.get(0);
                } else if ("detection_scores".equals(array.getName())) {
                    probabilities = array.get(0).toFloatArray();
                } else if ("detection_classes".equals(array.getName())) {
                    // class id is between 1 - number of classes
                    classIds = array.get(0).toType(DataType.INT32, true).toIntArray();
                }
            }
            Objects.requireNonNull(classIds);
            Objects.requireNonNull(probabilities);
            Objects.requireNonNull(boundingBoxes);

            List<String> retNames = new ArrayList<>();
            List<Double> retProbs = new ArrayList<>();
            List<BoundingBox> retBB = new ArrayList<>();

            // result are already sorted
            for (int i = 0; i < Math.min(classIds.length, maxBoxes); ++i) {
                int classId = classIds[i];
                double probability = probabilities[i];
                // classId starts from 1, -1 means background
                if (classId > 0 && probability > threshold) {
                    String className = classes.getOrDefault(classId, "#" + classId);
                    float[] box = boundingBoxes.get(i).toFloatArray();
                    float yMin = box[0];
                    float xMin = box[1];
                    float yMax = box[2];
                    float xMax = box[3];
                    Rectangle rect = new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin);
                    retNames.add(className);
                    retProbs.add(probability);
                    retBB.add(rect);
                }
            }

            return new DetectedObjects(retNames, retProbs, retBB);
        }
    }
}
