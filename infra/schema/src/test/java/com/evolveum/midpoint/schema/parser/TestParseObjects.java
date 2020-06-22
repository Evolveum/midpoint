/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
public class TestParseObjects extends AbstractParserTest {

    @Override
    protected File getFile() {
        return getFile(OBJECTS_FILE_BASENAME);
    }

    @Test
    public void testRoundTrip() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismParser parser = prismContext.parserFor(getFile());
        List<PrismObject<?>> objects = parser.parseObjects();

        System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

        assertEquals("Wrong # of objects", 4, objects.size());
        assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
        assertEquals("Wrong class of object 2", UserType.class, objects.get(1).asObjectable().getClass());
        assertEquals("Wrong class of object 3", RoleType.class, objects.get(2).asObjectable().getClass());
        assertEquals("Wrong class of object 4", ObjectCollectionType.class, objects.get(3).asObjectable().getClass());

        PrismSerializer<String> serializer = prismContext.serializerFor(language);
        String serializedByDefault = serializer.serializeObjects(objects, null);
        System.out.println("Objects as re-serialized (default method):\n" + serializedByDefault);

        System.out.println("Re-serialized to XML (default):\n" + prismContext.xmlSerializer().serializeObjects(objects, null));
        System.out.println("Re-serialized to JSON (default):\n" + prismContext.jsonSerializer().serializeObjects(objects, null));
        System.out.println("Re-serialized to YAML (default):\n" + prismContext.yamlSerializer().serializeObjects(objects, null));

        List<PrismObject<?>> objectsReparsedDefault = prismContext.parserFor(serializedByDefault).parseObjects();
        assertEquals("Reparsed objects are different from original ones", objects, objectsReparsedDefault);
    }

}
