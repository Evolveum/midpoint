/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.GenericTraceVisualizationType.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.traces.visualizer.TraceTreeVisualizer;
import com.evolveum.midpoint.schema.traces.visualizer.TraceVisualizerRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Experimental. Unfinished.
 */
@Experimental
public class TestParseTrace extends AbstractSchemaTest {

    private static final File TEST_DIR = new File("src/test/resources/traces");

    private static final File TRACE_MODIFY_GIVEN_NAME = new File(TEST_DIR, "trace-modify-given-name.zip");
    private static final File TRACE_MODIFY_EMPLOYEE_TYPE = new File(TEST_DIR, "trace-modify-employee-type.zip");
    private static final File TRACE_MODIFY_EMPLOYEE_TYPE_BUCCANEER = new File(TEST_DIR, "trace-modify-employee-type-buccaneer.zip");
    private static final File TRACE_MODIFY_COST_CENTER = new File(TEST_DIR, "trace-modify-cost-center.zip");
    private static final File TRACE_MODIFY_TELEPHONE_NUMBER = new File(TEST_DIR, "trace-modify-telephone-number.zip");
    private static final File TRACE_ASSIGN_DUMMY = new File(TEST_DIR, "trace-assign-dummy.zip");
    private static final File TRACE_RECONCILE_USER = new File(TEST_DIR, "trace-reconcile-user.zip");

    @Test
    public void test100ParseModifyGivenName() throws SchemaException, IOException {
        testParseTrace(TRACE_MODIFY_GIVEN_NAME);
    }

    @Test
    public void test110ParseModifyEmployeeType() throws SchemaException, IOException {
        testParseTrace(TRACE_MODIFY_EMPLOYEE_TYPE);
    }

    @Test
    public void test120ParseModifyEmployeeTypeBuccaneer() throws SchemaException, IOException {
        testParseTrace(TRACE_MODIFY_EMPLOYEE_TYPE_BUCCANEER);
    }

    @Test
    public void test130ParseModifyCostCenter() throws SchemaException, IOException {
        testParseTrace(TRACE_MODIFY_COST_CENTER);
    }

    @Test
    public void test140ParseModifyTelephoneNumber() throws SchemaException, IOException {
        testParseTrace(TRACE_MODIFY_TELEPHONE_NUMBER);
    }

    @Test
    public void test150ParseAssignDummy() throws SchemaException, IOException {
        testParseTrace(TRACE_ASSIGN_DUMMY);
    }

    @Test
    public void test160ParseReconcileUser() throws SchemaException, IOException {
        testParseTrace(TRACE_RECONCILE_USER);
    }

    private void testParseTrace(File file) throws IOException, SchemaException {
        given();
        PrismContext prismContext = getPrismContext();
        TraceParser parser = new TraceParser(prismContext);

        TraceVisualizerRegistry registry = new TraceVisualizerRegistry(prismContext);

        when();
        TracingOutputType parsed = parser.parse(file);
        List<OpNode> opNodeList = new OpNodeTreeBuilder(prismContext).build(parsed);
        TraceVisualizationInstructionsType overview = getInstructions(prismContext, ONE_LINE, false);
        TraceVisualizationInstructionsType overviewWithDurationBefore = showDurationBefore(getInstructions(prismContext, ONE_LINE, false));
        TraceVisualizationInstructionsType overviewWithColumns =
                showColumns(getInstructions(prismContext, ONE_LINE, false));
        TraceVisualizationInstructionsType all = getInstructions(prismContext, ONE_LINE, true);
        TraceVisualizationInstructionsType allWithColumns =
                showColumns(getInstructions(prismContext, ONE_LINE, true));
        TraceVisualizationInstructionsType brief = getInstructions(prismContext, BRIEF, false);
        TraceVisualizationInstructionsType briefWithColumns = showColumns(getInstructions(prismContext, BRIEF, false));
        TraceVisualizationInstructionsType detailed = getInstructions(prismContext, DETAILED, false);
        TraceVisualizationInstructionsType full = getInstructions(prismContext, FULL, true);

        then();

        TraceTreeVisualizer visualizer = new TraceTreeVisualizer(registry, opNodeList);

        opNodeList.forEach(node -> node.applyVisualizationInstructions(overview));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("OVERVIEW:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(overviewWithDurationBefore));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("OVERVIEW WITH DURATION BEFORE:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(overviewWithColumns));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("OVERVIEW WITH COLUMNS:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(all));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("ALL:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(allWithColumns));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("ALL WITH COLUMNS:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(brief));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("BRIEF:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(briefWithColumns));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("BRIEF WITH COLUMNS:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(detailed));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("DETAILED:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");

        opNodeList.forEach(node -> node.applyVisualizationInstructions(full));
        assertEquals("Wrong # of root opNodes", 1, opNodeList.size());
        System.out.println("FULL:\n");
        System.out.println(visualizer.visualize());

        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    private TraceVisualizationInstructionsType showColumns(TraceVisualizationInstructionsType instructions) {
        TraceVisualizationColumnsType columns = new TraceVisualizationColumnsType(getPrismContext());
        columns.setInvocationId(true);
        columns.setDuration(true);
        columns.getTimeFor().add(PerformanceCategory.REPOSITORY.name());
        columns.getTimeFor().add(PerformanceCategory.ICF.name());
        columns.getCountFor().add(PerformanceCategory.REPOSITORY_READ.name());
        columns.getCountFor().add(PerformanceCategory.REPOSITORY_WRITE.name());
        columns.getCountFor().add(PerformanceCategory.ICF_READ.name());
        columns.getCountFor().add(PerformanceCategory.ICF_WRITE.name());
        instructions.setColumns(columns);
        return instructions;
    }

    private TraceVisualizationInstructionsType showDurationBefore(TraceVisualizationInstructionsType instructions) {
        TraceVisualizationColumnsType columns = new TraceVisualizationColumnsType(getPrismContext());
        columns.setDurationBefore(true);
        instructions.setColumns(columns);
        return instructions;
    }

    private TraceVisualizationInstructionsType getInstructions(PrismContext prismContext, GenericTraceVisualizationType level,
            boolean other) {
        TraceVisualizationInstructionsType instructions = new TraceVisualizationInstructionsType(prismContext)
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.CLOCKWORK_EXECUTION)
                            .operationKind(OperationKindType.CLOCKWORK_CLICK)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(GenericTraceVisualizationType.ONE_LINE)
                        .<TraceVisualizationInstructionType>end()
                    .<TraceVisualizationInstructionsType>end()
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.MAPPING_EVALUATION)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(level)
                        .<TraceVisualizationInstructionType>end()
                    .<TraceVisualizationInstructionsType>end()
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.FOCUS_LOAD)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(level)
                        .<TraceVisualizationInstructionType>end()
                    .<TraceVisualizationInstructionsType>end()
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.FULL_PROJECTION_LOAD)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(level)
                        .<TraceVisualizationInstructionType>end()
                    .<TraceVisualizationInstructionsType>end()
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.FOCUS_CHANGE_EXECUTION)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(level)
                        .<TraceVisualizationInstructionType>end()
                    .<TraceVisualizationInstructionsType>end()
                    .beginInstruction()
                        .beginSelector()
                            .operationKind(OperationKindType.PROJECTION_CHANGE_EXECUTION)
                        .<TraceVisualizationInstructionType>end()
                        .beginVisualization()
                            .generic(level)
                        .<TraceVisualizationInstructionType>end()
                    .end();
        if (other) {
            instructions.getInstruction().add(
                    new TraceVisualizationInstructionType(prismContext)
                            .beginVisualization()
                               .generic(ONE_LINE)
                            .end());
        }
        return instructions;
    }
}
