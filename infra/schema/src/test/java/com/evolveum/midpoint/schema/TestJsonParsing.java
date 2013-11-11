package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.FileInputStream;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.json.PrismJasonProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

public class TestJsonParsing {

	// private PrismJasonProcessor jsonProcessor;

	@Test
	public void testParseUserJson() throws Exception {
		System.out.println("===[ testParseUserJson ]===");
		PrismJasonProcessor jsonProcessor = new PrismJasonProcessor();
		PrismContext prismContext = MidPointPrismContextFactory.FACTORY
				.createEmptyPrismContext();
		prismContext.initialize();
		jsonProcessor.setPrismContext(prismContext);
		jsonProcessor.setSchemaRegistry(prismContext.getSchemaRegistry());
		// PrismTestUtil.setFactory(MidPointPrismContextFactory.FACTORY);
		// PrismContext prismContext = PrismTestUtil.createPrismContext();
		// jsonProcessor.setPrismContext(prismContext);
		PrismObject<UserType> userType = jsonProcessor.parseObject(new FileInputStream(
				new File("src/test/resources/common/json/user-jack.json")),
				UserType.class);
		
		System.out.println("parsed");

//		PrismObject<UserType> user = userType.asPrismObject();
		System.out.println("object");
		System.out.println(userType.dump());
	}
}
