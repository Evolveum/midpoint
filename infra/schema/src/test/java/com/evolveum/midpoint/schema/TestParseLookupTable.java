/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Viliam Repan (lazyman)
 */
public class TestParseLookupTable extends AbstractSchemaTest {

    public static final File LOOKUP_TABLE_FILE = new File("src/test/resources/common/lookup-table.xml");

    @Test
    public void testParseTableFileRoundTrip() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<LookupTableType> table = prismContext.parserFor(LOOKUP_TABLE_FILE).xml().parse();

        // THEN
        System.out.println("Parsed table:");
        System.out.println(table.debugDump());

        assertTable(table);

        // WHEN
        String xml = prismContext.xmlSerializer().serialize(table);

        // THEN
        System.out.println("Serialized object:");
        System.out.println(xml);

        // WHEN
        PrismObject<ObjectTemplateType> reparsedObject = prismContext.parseObject(xml);

        // THEN
        System.out.println("Re-parsed object:");
        System.out.println(reparsedObject.debugDump());

        assertTable(table);
        PrismAsserts.assertEquals(table.asObjectable(), reparsedObject.asObjectable());
    }

    private void assertTable(PrismObject<LookupTableType> table) {
        table.checkConsistence();

        assertEquals("Wrong oid", "44444444-4444-4444-4444-000000001111", table.getOid());
        assertPropertyValue(table, "name", PrismTestUtil.createPolyString("first lookup"));
        assertPropertyDefinition(table, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
        assertPropertyValue(table, "description", "description of lookup table");
        assertPropertyDefinition(table, "description", DOMUtil.XSD_STRING, 0, 1);
        PrismContainer<LookupTableRowType> tableContainer = table.findContainer(LookupTableType.F_ROW);
        assertEquals("wrong number of rows", 2, tableContainer.size());
        assertRow(tableContainer.getValues().get(0),
                "first key",
                "first value",
                PrismTestUtil.createPolyStringType("first label"),
                XmlTypeConverter.createXMLGregorianCalendar("2013-05-07T10:38:21.350+02:00"));
        assertRow(tableContainer.getValues().get(1),
                "2 key",
                "2 value",
                PrismTestUtil.createPolyStringType("second ľábeľ", "second label"),
                XmlTypeConverter.createXMLGregorianCalendar("2013-05-07T10:40:21.350+02:00"));
    }

    private void assertRow(PrismContainerValue<LookupTableRowType> tableContainerValue, String key, String value, PolyStringType label, XMLGregorianCalendar lastChangeTimestamp) {
        LookupTableRowType row = tableContainerValue.asContainerable();
        assertEquals("wrong key", key, row.getKey());
        assertEquals("wrong value", value, row.getValue());
        assertEquals("wrong label", label, row.getLabel());
        assertEquals("wrong lastChangeTimestamp", lastChangeTimestamp, row.getLastChangeTimestamp());
    }

    private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
            int maxOccurs) {
        QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }
}
