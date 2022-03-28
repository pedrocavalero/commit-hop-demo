package br.com.javaparainiciantes.commit.hop.controller;

import java.io.File;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@AllArgsConstructor
public class ObjectDetectionController {
	
	
	ObjectDetectionWithTensorflowSavedModelService detectorService;

	@RequestMapping("/object-detection")
	public String formDetection(Model model) {
		return "object-detection/object-detection-form";
	}

	@PostMapping("/object-detection/execute")
	public String executeDetection(@ModelAttribute ObjectDetectionDto dto, Model model) {        
        try
        {
        	Image image = ImageFactory.getInstance().fromInputStream(dto.getImage().getInputStream());
        	detectorService.predict(dto, image);
        	model.addAttribute("dto",dto);
        	
        } catch (Exception e) {
            model.addAttribute("error", e);
            throw new RuntimeException(e);
        }
        
		return "object-detection/object-detection-show";
	}
}
