package br.com.javaparainiciantes.commit.hop.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;

@Controller
public class ObjectDetectionMobilenetController {

    private static final String PNG = ".png";

    @Resource
    private Supplier<Predictor<Image, DetectedObjects>> predictorSupplier;

    @Resource
    private ImageFactory imageFactory;

    
	@RequestMapping("/object-detection-mobilenet")
	public String formDetection(Model model) {
		return "object-detection-mobilenet/object-detection-form";
	}

	@PostMapping("/object-detection-mobilenet/execute")
	public String executeDetection(@ModelAttribute ObjectDetectionDto dto, Model model) {  
    	
        try(var p = predictorSupplier.get()) {
        	Image image = ImageFactory.getInstance().fromInputStream(dto.getImage().getInputStream());
            var detected = p.predict(image);
            Image newImage = createImage(detected, image);
            saveImageBase64(dto, newImage);
            model.addAttribute("dto",dto);
        } catch (IOException|TranslateException e) {
            model.addAttribute("error", e);
            throw new RuntimeException(e);
		} 
		
        
		return "object-detection-mobilenet/object-detection-show";
	}

	private void saveImageBase64(ObjectDetectionDto dto, Image newImage) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Base64OutputStream b64os = new Base64OutputStream(baos);
		newImage.save(b64os, "png");
		b64os.close();
		dto.setImageBase64("data:image/png;base64,"+baos.toString());
	}

    private Image createImage(DetectedObjects detection, Image original) {
        Image newImage = original.duplicate();
        newImage.drawBoundingBoxes(detection);
		return newImage;
	}

}
