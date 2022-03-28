package br.com.javaparainiciantes.commit.hop;

import java.io.IOException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.modality.cv.Image;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;

@Configuration
public class ObjectDetectionMobilenetConfiguration {

    @Bean
    public ImageFactory imageFactory() {
        return ImageFactory.getInstance();
    }

    @Bean
    public Criteria<Image, DetectedObjects> criteria() {
        return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
//                .optFilter("size", "512")
//                .optFilter("backbone", "mobilenet1.0")
//                .optFilter("dataset", "voc")
                .optArgument("threshold", 0.1)
                .build();
    }

    @Bean
    public ZooModel<Image, DetectedObjects> model(
            @Qualifier("criteria") Criteria<Image, DetectedObjects> criteria)
            throws MalformedModelException, ModelNotFoundException, IOException {
        return ModelZoo.loadModel(criteria);
    }

    /**
     * Scoped proxy is one way to have a predictor configured and closed.
     * @param model object for which predictor is expected to be returned
     * @return predictor object that can be used for inference
     */
    @Bean(destroyMethod = "close")
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
    public Predictor<Image, DetectedObjects> predictor(ZooModel<Image, DetectedObjects> model) {
        return model.newPredictor();
    }

    /**
     * Inject with @Resource or autowired. Only safe to be used in the try with resources.
     * @param model object for which predictor is expected to be returned
     * @return supplier of predictor for thread-safe inference
     */
    @Bean
    public Supplier<Predictor<Image, DetectedObjects>> predictorProvider(ZooModel<Image, DetectedObjects> model) {
        return model::newPredictor;
    }
}
