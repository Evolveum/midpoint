/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestParseFilter {

	public static final File FILTER_FILE = new File(TestConstants.COMMON_DIR, "filter.xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}


	@Test
	public void testParseFilterFile() throws Exception {
		System.out.println("===[ testParseFilterFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		SearchFilterType filter = prismContext.parserFor(FILTER_FILE).parseRealValue(SearchFilterType.class);

		// THEN
		System.out.println("Parsed filter:");
		System.out.println(filter.debugDump());

        String serialized = PrismTestUtil.serializeJaxbElementToString(new JAXBElement<>(
            new QName(SchemaConstants.NS_QUERY, "filter"), SearchFilterType.class, filter));
        System.out.println("JAXB serialization result:\n"+serialized);

        // WHEN2

        SearchFilterType filter2 = prismContext.parserFor(serialized).parseRealValue(SearchFilterType.class);

        System.out.println("Reparsed filter:");
        System.out.println(filter2.debugDump());

        // THEN2

        assertEquals("Parsed and serialized+parsed filters do not match", filter, filter2);
    }

}
