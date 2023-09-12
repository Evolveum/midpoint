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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class TestMerger extends AbstractSchemaTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestMerger.class);

    private static final File TEST_ROOT_DIR = new File("./src/test/resources/merger");

    @Test
    public void test10LookupTableMergeOperation() throws Exception {
        testMergeOperation("lookup-table/lookup-table");
    }

    @Test
    public void test20SecurityPolicyMergeOperation() throws Exception {
        testMergeOperation("security-policy/security-policy");
    }

    @Test
    public void test30SystemConfigurationMergeOperation() throws Exception {
        testMergeOperation("system-configuration/system-configuration");
    }

    @Test
    public void test40RoleEndUserMergeOperation() throws Exception {
        testMergeOperation("role/role-enduser");
    }

    @Test
    public void test50RoleSuperuserMergeOperation() throws Exception {
        testMergeOperation("role/role-superuser");
    }

    @Test
    public void test60TaskMergeOperation() throws Exception {
        testMergeOperation("task/task-validity");
    }

    @Test
    public void test70ReportMergeOperation() throws Exception {
        testMergeOperation("report/report-certification-campaigns");
    }

    @Test
    public void test80ObjectCollectionMergeOperation() throws Exception {
        testMergeOperation("object-collection/object-collection-resource-up");
    }

    @Test
    public void test90DashboardMergeOperation() throws Exception {
        testMergeOperation("dashboard/dashboard-admin");
    }

    @Test
    public void test100UserMergeOperation() throws Exception {
        testMergeOperation("user/user-administrator");
    }

    @Test
    public void test110ArchetypeMergeOperation() throws Exception {
        testMergeOperation("archetype/archetype-task-live-sync");
    }

    @Test
    public void test120MarkMergeOperation() throws Exception {
        testMergeOperation("mark/mark-focus-deactivated");
    }

    @Test
    public void test130ValuePolicyMergeOperation() throws Exception {
        testMergeOperation("value-policy/value-policy");
    }

    private <O extends ObjectType> void testMergeOperation(String fileNamePrefix) throws IOException, SchemaException, ConfigurationException {
        PrismObject<O> source = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-source.xml"));
        PrismObject<O> target = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-target.xml"));
        PrismObject<O> result = getPrismContext().parseObject(new File(TEST_ROOT_DIR, fileNamePrefix + "-result.xml"));

        SimpleObjectMergeOperation.merge(target, source);

        LOGGER.trace("Merged object:\n{}", target.debugDump());
        LOGGER.trace("Result object:\n{}", result.debugDump());

        ObjectDelta<O> delta = target.diff(result);

        Assertions.assertThat(target)
                .matches(
                        t -> t.equivalent(result),
                        "Merged object is not equivalent to expected result\n" + delta.debugDump());
    }
}
