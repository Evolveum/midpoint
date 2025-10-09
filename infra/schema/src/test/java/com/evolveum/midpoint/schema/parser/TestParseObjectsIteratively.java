/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;

public class TestParseObjectsIteratively extends AbstractParserTest {

    @Override
    protected File getFile() {
        return getFile(OBJECTS_FILE_BASENAME);
    }

    @Test
    public void testRoundTrip() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismParser parser = prismContext.parserFor(getFile());
        List<PrismObject<?>> objects = new ArrayList<>();
        parser.parseObjectsIteratively(new PrismParser.ObjectHandler() {
            @Override
            public boolean handleData(PrismObject<?> object) {
                objects.add(object);
                return true;
            }

            @Override
            public boolean handleError(Throwable t) {
                throw new AssertionError("unexpected handleError call");
            }
        });

        System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

        assertEquals("Wrong # of objects", 4, objects.size());
        assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
        assertEquals("Wrong class of object 2", UserType.class, objects.get(1).asObjectable().getClass());
        assertEquals("Wrong class of object 3", RoleType.class, objects.get(2).asObjectable().getClass());
        assertEquals("Wrong class of object 4", ObjectCollectionType.class, objects.get(3).asObjectable().getClass());

        List<PrismObject<?>> objectsStandard = prismContext.parserFor(getFile()).parseObjects();
        assertEquals("Objects are different if read in a standard way", objectsStandard, objects);
    }

}
