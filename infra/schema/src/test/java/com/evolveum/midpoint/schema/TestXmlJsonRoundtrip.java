package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestXmlJsonRoundtrip extends AbstractSchemaTest {

    private static final File TEST_FOLDER = new File("src/test/resources/xmljson/");

    @DataProvider(name = "xmlJsonFiles")
    public static Object[][] testFiles() {
        String[] files = TEST_FOLDER.list((d,n) -> n.endsWith(".xml"));
        Object[][] ret = new Object[files.length][];
        int o = 0;
        for(String name: files) {

            File file = new File(TEST_FOLDER, name);
            ret[o] = new Object[] { file};
            o++;
        }
        return ret;
    }


    @Test(dataProvider = "xmlJsonFiles")
    void testFile(File name) throws SchemaException, IOException {
        PrismContext context = getPrismContext();
        PrismObject<Objectable> xmlObject = context.parserFor(name).language("xml").parse();
        assertNotNull(xmlObject);

        String jsonString = context.jsonSerializer().serialize(xmlObject);
        display(jsonString);
        @NotNull
        PrismObject<Objectable> jsonObject = context.parserFor(jsonString).language("json").parse();

        assertObjectEquals(jsonObject,xmlObject);


        String json2String = context.jsonSerializer().serialize(jsonObject);
        var json2Object = context.parserFor(json2String).language("json").parse();
        assertObjectEquals(jsonObject, json2Object);

    }

    private void assertObjectEquals(@NotNull PrismObject<Objectable> jsonObject, PrismObject<Objectable> xmlObject) {
        if(!xmlObject.equals(jsonObject)) {
            ObjectDelta<Objectable> diff = xmlObject.diff(jsonObject);
            display("Fail");
            display(diff.debugDump());
            fail("Parsed objects are different");
        }
    }


    public static PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }
}
