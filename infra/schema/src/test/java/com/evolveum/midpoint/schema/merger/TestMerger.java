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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.FileUtils;
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

    // not commited, contains inital objects for 4.4 and master in separate folders
    private static final File TEST_DIR = new File("../../_mess/_init-objects-diff");

    @Test(enabled = false)
    public <O extends ObjectType> void testInitialObjects() throws Exception {
        Map<String, PrismObject<O>> source = loadPrismObjects(new File(TEST_DIR, "master"));
        Map<String, PrismObject<O>> target = loadPrismObjects(new File(TEST_DIR, "support-4.4"));

        int successCount = 0;
        int errorCount = 0;
        for (String oid : source.keySet()) {
            PrismObject<O> sourceObject = source.get(oid);
            PrismObject<O> targetObject = target.get(oid);
            if (targetObject == null) {
                continue;
            }

            try {
                ObjectMergeOperation.merge(targetObject, sourceObject);
                successCount++;
            } catch ( Exception ex) {
                errorCount++;
                ex.printStackTrace();
                System.out.println("Couldn't merge object " + sourceObject.toDebugName());
            }
        }

        Assertions.assertThat(errorCount)
                .withFailMessage("Successfully merged <%s> objects, <%s> errors", successCount, errorCount)
                .isZero();
    }

    private <O extends ObjectType> Map<String, PrismObject<O>> loadPrismObjects(File dir) {
        Map<String, PrismObject<O>> result = new HashMap<>();

        Collection<File> files = FileUtils.listFiles(dir, new String[]{"xml"}, true);
        files.stream()
                .map(f -> {
                    try {
                        return (PrismObject) getPrismContext().parseObject(f);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach(o -> result.put(o.getOid(), o));

        return result;
    }

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

    @Test
    public void test40RoleEndUserMergeOperation() throws Exception {
        testMergeOperation("role-enduser");
    }

    private void testMergeOperation(String fileNamePrefix) throws IOException, SchemaException, ConfigurationException {
        PrismObject<LookupTableType> source = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-source.xml"));
        PrismObject<LookupTableType> target = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-target.xml"));
        PrismObject<LookupTableType> result = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-result.xml"));

        ObjectMergeOperation.merge(target, source);

        LOGGER.trace("Merged object:\n{}", target.debugDump());

        Assertions.assertThat(target)
                .matches(t -> t.equivalent(result), result.debugDump());
    }
}
