package br.com.javaparainiciantes.commit.hop.controller;

import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ObjectDetectionDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private String modelUrl;
	
	private MultipartFile image;
	
	private String inputDataType;
	
	private int inputWidth;

	private int inputHeigth;
	
	private String imageBase64;
	
}
