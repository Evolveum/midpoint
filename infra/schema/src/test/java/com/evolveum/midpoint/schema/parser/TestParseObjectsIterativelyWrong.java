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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_WRONG_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
public class TestParseObjectsIterativelyWrong extends AbstractParserTest {

    @Override
    protected File getFile() {
        return getFile(OBJECTS_WRONG_FILE_BASENAME);
    }

    @Test
    public void testParse() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismParser parser = prismContext.parserFor(getFile());
        List<PrismObject<?>> objects = new ArrayList<>();
        AtomicInteger errors = new AtomicInteger(0);
        parser.parseObjectsIteratively(new PrismParser.ObjectHandler() {
            @Override
            public boolean handleData(PrismObject<?> object) {
                objects.add(object);
                return true;
            }

            @Override
            public boolean handleError(Throwable t) {
                System.out.println("Got (probably expected) exception:");
                t.printStackTrace(System.out);
                assert t instanceof SchemaException;
                errors.incrementAndGet();
                return true;
            }
        });

        System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

        assertEquals("Wrong # of objects", 2, objects.size());
        assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
        assertEquals("Wrong class of object 2", RoleType.class, objects.get(1).asObjectable().getClass());

        assertEquals("Wrong # of errors", 1, errors.get());

        try {
            prismContext.parserFor(getFile()).parseObjects();
            fail("unexpected success");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e);
        }
    }

}
