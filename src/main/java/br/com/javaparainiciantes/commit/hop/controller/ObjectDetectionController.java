package br.com.javaparainiciantes.commit.hop.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
	public String executeDetection(HttpServletRequest servletRequest, @ModelAttribute ObjectDetectionDto dto, Model model) {
        String fileName = dto.getImage().getOriginalFilename();

        File imageFile = new File(servletRequest.getServletContext().getRealPath("/image"), fileName);
        try
        {
        	dto.getImage().transferTo(imageFile);
        	Path predicted = detectorService.predict(dto, imageFile);
        	model.addAttribute("dto",dto);
        	model.addAttribute("image", "/image/"+predicted.getFileName());
        	
        } catch (Exception e) {
            model.addAttribute("error", e);
            throw new RuntimeException(e);
        }
        
		return "object-detection/object-detection-show";
	}
}
