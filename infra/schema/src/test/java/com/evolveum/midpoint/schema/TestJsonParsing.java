package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.json.PrismJasonProcessor;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.exception.SchemaException;
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
