package com.evolveum.midpoint.schema.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLDecoder;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SerializedDomToSchemaProcessorTest {

	private static final Trace LOGGER = TraceManager.getTrace(SerializedDomToSchemaProcessorTest.class);

	@Test
	public void serializedTest() throws Exception {
		URL url = SerializedDomToSchemaProcessorTest.class.getClassLoader().getResource(
				"processor/serialized");
		File folder = new File(url.toURI());
		File[] files = folder.listFiles();
		for (File file : files) {			
			try {
				LOGGER.debug("Deserializing file {}", URLDecoder.decode(file.getName(), "utf-8"));
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
				Schema schema = (Schema) in.readObject();
				in.close();
				LOGGER.debug("Parsing schema to dom (dom in trace).");
				Document document = schema.serializeToXsd();
				LOGGER.trace("Schema parsed to dom:\n{}", DOMUtil.printDom(document));
				LOGGER.debug("Parsing dom schema to object");
				schema = Schema.parse(document.getDocumentElement());
				LOGGER.debug("Schema parsed {}", schema);
			} catch (Exception ex) {
				Assert.fail(ex.getMessage(), ex);
			}
		}
	}
}
