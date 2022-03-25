package br.com.javaparainiciantes.commit.hop.controller;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ObjectDetectionController {

	@RequestMapping("/object-detection")
	public String formDetection(Model model) {
		return "object-detection/object-detection-form";
	}

	@PostMapping("/object-detection/execute")
	public String executeDetection(HttpServletRequest servletRequest, @ModelAttribute ObjectDetectionDto dto, Model model) {
		log.info(dto.toString());
        String fileName = dto.getImage().getOriginalFilename();

        File imageFile = new File(servletRequest.getServletContext().getRealPath("/image"), fileName);
        try
        {
        	dto.getImage().transferTo(imageFile);
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
		model.addAttribute("dto",dto);
		model.addAttribute("image", "/image/"+imageFile.getName());
		return "object-detection/object-detection-show";
	}
}
