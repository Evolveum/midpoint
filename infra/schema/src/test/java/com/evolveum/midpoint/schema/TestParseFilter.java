/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class TestParseFilter extends AbstractSchemaTest {

    public static final File FILTER_FILE = new File(TestConstants.COMMON_DIR, "filter.xml");

    @Test
    public void testParseFilterFile() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        SearchFilterType filter = prismContext.parserFor(FILTER_FILE).parseRealValue(SearchFilterType.class);

        // THEN
        System.out.println("Parsed filter:");
        System.out.println(filter.debugDump());

        String serialized = PrismTestUtil.serializeJaxbElementToString(new JAXBElement<>(
                new QName(SchemaConstants.NS_QUERY, "filter"), SearchFilterType.class, filter));
        System.out.println("JAXB serialization result:\n" + serialized);

        // WHEN2

        SearchFilterType filter2 = prismContext.parserFor(serialized).parseRealValue(SearchFilterType.class);

        System.out.println("Reparsed filter:");
        System.out.println(filter2.debugDump());

        // THEN2

        assertEquals("Parsed and serialized+parsed filters do not match", filter, filter2);
    }
}
