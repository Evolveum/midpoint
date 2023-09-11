/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.merger.object.ObjectMergeOperation;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class TestMerger extends AbstractSchemaTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestMerger.class);

    private static final File TEST_ROOT_DIR = new File("./src/test/resources/merger");

    @Test
    public void test10LookupTableMergeOperation() throws Exception {
        testMergeOperation("lookup-table");
    }

    @Test
    public void test20SecurityPolicyMergeOperation() throws Exception {
        testMergeOperation("security-policy");
    }

    @Test
    public void test30SystemConfigurationMergeOperation() throws Exception {
        testMergeOperation("system-configuration");
    }

    private void testMergeOperation(String fileNamePrefix) throws IOException, SchemaException, ConfigurationException {
        PrismObject<LookupTableType> source = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-source.xml"));
        PrismObject<LookupTableType> target = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-target.xml"));
        PrismObject<LookupTableType> result = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-result.xml"));

        ObjectMergeOperation.merge(target, source);

        LOGGER.trace("Merged object:\n{}", target.debugDump());

        Assertions.assertThat(target)
                .matches(t -> t.equivalent(result));
    }
}
