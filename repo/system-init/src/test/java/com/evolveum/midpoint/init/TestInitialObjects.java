/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.merger.SimpleObjectMergeOperation;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class TestInitialObjects extends AbstractUnitTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestInitialObjects.class);

    private static final File INITIAL_OBJECTS_DIR = new File("./src/main/resources/initial-objects");

    private int success = 0;
    private int failed = 0;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void mergeInitialObjects() throws Exception {
        testMergeOnFiles(INITIAL_OBJECTS_DIR.listFiles());

        LOGGER.info("Success: " + success + ", Failed: " + failed + ", Total: " + (success + failed));

        Assertions.assertThat(failed).withFailMessage(() -> "Failed " + failed + " merged initial objects.")
                .matches(t -> t == 0);
    }

    public void testMergeOnFiles(File... files) throws SchemaException, IOException, ConfigurationException {
        for (File file : files) {
            if (file.isDirectory()) {
                testMergeOnFiles(file.listFiles());
            } else {
                testMergeOnFile(file);
                success++;
            }
        }
    }

    private <O extends ObjectType> void testMergeOnFile(File file) throws SchemaException, IOException, ConfigurationException {
        PrismObject<O> object = getPrismContext().parseObject(file);
        PrismObject<O> objectBeforeMerge = object.cloneComplex(CloneStrategy.REUSE);

        LOGGER.trace("Object before merge:\n{}", objectBeforeMerge.debugDump());

        SimpleObjectMergeOperation.merge(object, objectBeforeMerge);

        LOGGER.trace("Object after merge:\n{}", object.debugDump());

        ObjectDelta<O> delta = objectBeforeMerge.diff(object);

        try {
            Assertions.assertThat(object)
                    .matches(
                            t -> t.equivalent(objectBeforeMerge));
            success++;
        } catch (AssertionError e) {
            failed++;
            LOGGER.error("Merged object is not equivalent to expected result\n" + delta.debugDump(), e);
        }
    }
}
