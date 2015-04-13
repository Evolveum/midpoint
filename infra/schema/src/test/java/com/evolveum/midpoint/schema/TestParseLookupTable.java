package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Viliam Repan (lazyman)
 */
public class TestParseLookupTable {

    public static final File LOOKUP_TABLE_FILE = new File("src/test/resources/common/lookup-table.xml");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }


    @Test
    public void testParseTableFileRoundTrip() throws Exception {
        System.out.println("===[ testParseTableFileRoundTrip ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<LookupTableType> table = prismContext.parseObject(LOOKUP_TABLE_FILE, PrismContext.LANG_XML);

        // THEN
        System.out.println("Parsed table:");
        System.out.println(table.debugDump());

        assertTable(table);

        // WHEN
        String xml = prismContext.serializeObjectToString(table, PrismContext.LANG_XML);

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
        assertRow(tableContainer.getValue(0),
                "first key",
                "first value",
                PrismTestUtil.createPolyStringType("first label"),
                XmlTypeConverter.createXMLGregorianCalendar("2013-05-07T10:38:21.350+02:00"));
        assertRow(tableContainer.getValue(1),
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
        QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }
}
