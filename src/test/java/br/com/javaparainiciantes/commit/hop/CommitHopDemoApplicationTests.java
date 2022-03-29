package br.com.javaparainiciantes.commit.hop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.javaparainiciantes.commit.hop.controller.ObjectDetectionWithTensorflowSavedModelService;

class CommitHopDemoApplicationTests {

	@Test
	void labelMapLoadTest() throws IOException {
		Map<Integer,String> synset = ObjectDetectionWithTensorflowSavedModelService.MyTranslator.loadSynset();
		assertEquals("person", synset.get(1));
		assertEquals("motorcycle", synset.get(4));
	}

}
