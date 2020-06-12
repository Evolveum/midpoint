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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
public class TestParseObjectsIterativelyFirstTwo extends AbstractParserTest {

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
                return objects.size() != 2;
            }

            @Override
            public boolean handleError(Throwable t) {
                throw new AssertionError("unexpected handleError call");
            }
        });

        System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

        assertEquals("Wrong # of objects", 2, objects.size());
        assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
        assertEquals("Wrong class of object 2", UserType.class, objects.get(1).asObjectable().getClass());

        List<PrismObject<?>> objectsStandard = prismContext.parserFor(getFile()).parseObjects();
        objectsStandard.remove(3);
        objectsStandard.remove(2);
        assertEquals("Objects are different if read in a standard way", objectsStandard, objects);
    }

}
