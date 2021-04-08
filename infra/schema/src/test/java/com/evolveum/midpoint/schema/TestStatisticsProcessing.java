/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.parseObject;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.statistics.StructuredTaskProgress;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.schema.util.task.TaskProgressUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StructuredTaskProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests various aspects of statistics processing.
 */
public class TestStatisticsProcessing extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/statistics");
    private static final File TASK_AGGREGATION_ROOT = new File(TEST_DIR, "task-aggregation-root.xml");
    private static final File TASK_AGGREGATION_CHILD_1 = new File(TEST_DIR, "task-aggregation-child-1.xml");
    private static final File TASK_AGGREGATION_CHILD_2 = new File(TEST_DIR, "task-aggregation-child-2.xml");

    /**
     * Tests mainly for MID-6975 (colliding IDs in task statistics).
     */
    @Test
    public void testAggregation() throws Exception {
        given();

        PrismContext prismContext = getPrismContext();

        PrismObject<TaskType> root = parseObject(TASK_AGGREGATION_ROOT);
        PrismObject<TaskType> child1 = parseObject(TASK_AGGREGATION_CHILD_1);
        PrismObject<TaskType> child2 = parseObject(TASK_AGGREGATION_CHILD_2);
        root.asObjectable().getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(child1, prismContext));
        root.asObjectable().getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(child2, prismContext));

        when();

        OperationStatsType treeStats = TaskOperationStatsUtil.getOperationStatsFromTree(root.asObjectable(), prismContext);
        int itemsProcessed = TaskOperationStatsUtil.getItemsProcessed(treeStats);
        StructuredTaskProgressType treeProgress = TaskProgressUtil.getStructuredProgressFromTree(root.asObjectable());

        then();

        assertThat(itemsProcessed).as("items processed").isEqualTo(14);
        displayValue("aggregated statistics", TaskOperationStatsUtil.format(treeStats));
        displayValue("aggregated progress", StructuredTaskProgress.format(treeProgress));
    }
}
