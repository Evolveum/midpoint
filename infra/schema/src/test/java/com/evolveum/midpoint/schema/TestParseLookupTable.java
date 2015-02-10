package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

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
    public void testParseTableFile() throws Exception {
        System.out.println("===[ testParseTableFile ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<LookupTableType> task = prismContext.parseObject(LOOKUP_TABLE_FILE, PrismContext.LANG_XML);

        // THEN
        System.out.println("Parsed table:");
        System.out.println(task.debugDump());

        assertTask(task);
    }

    private void assertTask(PrismObject<LookupTableType> table) {
        table.checkConsistence();

        assertEquals("Wrong oid", "44444444-4444-4444-4444-000000001111", table.getOid());

        assertPropertyValue(table, "name", PrismTestUtil.createPolyString("Example Task"));
        assertPropertyDefinition(table, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        //todo more asserts
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
