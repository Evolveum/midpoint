/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.traces.PerformanceCategory;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;

import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.authentication.api.authorization.Url;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpNodeTreeBuilder;
import com.evolveum.midpoint.schema.traces.TraceParser;
import com.evolveum.midpoint.schema.traces.visualizer.TraceTreeVisualizer;
import com.evolveum.midpoint.schema.traces.visualizer.TraceVisualizerRegistry;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.TraceViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 *
 */
@Experimental
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/traceView", matchUrlForSecurity = "/admin/config/traceView")
        }, action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TRACE_VIEW_URL,
                        label = "PageTraceView.auth.view.label", description = "PageTraceView.auth.view.description")
        }, experimental = true)
public class PageTraceView extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageTraceView.class);

    public static final String PARAM_OBJECT_ID = "oid";

    private static final String DOT_CLASS = PageTraceView.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_SHOW = "show";
    private static final String ID_RESULT_LABEL = "resultLabel";
    private static final String ID_RESULT_TEXT = "resultText";
    private static final String ID_CLOCKWORK_EXECUTION = "clockworkExecution";
    private static final String ID_CLOCKWORK_CLICK = "clockworkClick";
    private static final String ID_MAPPING_EVALUATION = "mappingEvaluation";
    private static final String ID_FOCUS_LOAD = "focusLoad";
    private static final String ID_PROJECTION_LOAD = "projectionLoad";
    private static final String ID_FOCUS_CHANGE = "focusChange";
    private static final String ID_PROJECTION_CHANGE = "projectionChange";
    private static final String ID_OTHERS = "others";

    private static final String ID_SHOW_INVOCATION_ID = "showInvocationId";
    private static final String ID_SHOW_DURATION_BEFORE = "showDurationBefore";
    private static final String ID_SHOW_DURATION_AFTER = "showDurationAfter";
    private static final String ID_SHOW_REPO_OP_COUNT = "showRepoOpCount";
    private static final String ID_SHOW_CONN_ID_OP_COUNT = "showConnIdOpCount";
    private static final String ID_SHOW_REPO_OP_TIME = "showRepoOpTime";
    private static final String ID_SHOW_CONN_ID_OP_TIME = "showConnIdOpTime";

    private static final String OP_VISUALIZE = DOT_CLASS + "visualize";

    private transient List<OpNode> parsedOpNodeList;
    private transient String parsedFilePath;

    private final Model<TraceViewDto> model = new Model<>(new TraceViewDto());

    public PageTraceView() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        DropDownChoicePanel<GenericTraceVisualizationType> clockworkExecutionChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_CLOCKWORK_EXECUTION, createClockworkLevels(),
                new PropertyModel<>(model, TraceViewDto.F_CLOCKWORK_EXECUTION),
                this, false);
        clockworkExecutionChoice.setOutputMarkupId(true);
        mainForm.add(clockworkExecutionChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> clockworkClickChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_CLOCKWORK_CLICK, createClockworkLevels(),
                new PropertyModel<>(model, TraceViewDto.F_CLOCKWORK_CLICK),
                this, false);
        clockworkClickChoice.setOutputMarkupId(true);
        mainForm.add(clockworkClickChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> mappingEvaluationChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_MAPPING_EVALUATION, createMappingLevels(),
                new PropertyModel<>(model, TraceViewDto.F_MAPPING_EVALUATION),
                this, false);
        mappingEvaluationChoice.setOutputMarkupId(true);
        mainForm.add(mappingEvaluationChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> focusLoadChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_FOCUS_LOAD, createStandardLevels(),
                new PropertyModel<>(model, TraceViewDto.F_FOCUS_LOAD),
                this, false);
        focusLoadChoice.setOutputMarkupId(true);
        mainForm.add(focusLoadChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> projectionLoadChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_PROJECTION_LOAD, createStandardLevels(),
                new PropertyModel<>(model, TraceViewDto.F_PROJECTION_LOAD),
                this, false);
        projectionLoadChoice.setOutputMarkupId(true);
        mainForm.add(projectionLoadChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> focusChangeChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_FOCUS_CHANGE, createStandardLevels(),
                new PropertyModel<>(model, TraceViewDto.F_FOCUS_CHANGE),
                this, false);
        focusChangeChoice.setOutputMarkupId(true);
        mainForm.add(focusChangeChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> projectionChangeChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_PROJECTION_CHANGE, createStandardLevels(),
                new PropertyModel<>(model, TraceViewDto.F_PROJECTION_CHANGE),
                this, false);
        projectionChangeChoice.setOutputMarkupId(true);
        mainForm.add(projectionChangeChoice);

        DropDownChoicePanel<GenericTraceVisualizationType> otherChoice = WebComponentUtil.createEnumPanel(
                GenericTraceVisualizationType.class, ID_OTHERS, createOthersLevels(),
                new PropertyModel<>(model, TraceViewDto.F_OTHERS),
                this, false);
        otherChoice.setOutputMarkupId(true);
        mainForm.add(otherChoice);

        mainForm.add(new CheckBox(ID_SHOW_INVOCATION_ID, new PropertyModel<>(model, TraceViewDto.F_SHOW_INVOCATION_ID)));
        mainForm.add(new CheckBox(ID_SHOW_DURATION_BEFORE, new PropertyModel<>(model, TraceViewDto.F_SHOW_DURATION_BEFORE)));
        mainForm.add(new CheckBox(ID_SHOW_DURATION_AFTER, new PropertyModel<>(model, TraceViewDto.F_SHOW_DURATION_AFTER)));
        mainForm.add(new CheckBox(ID_SHOW_REPO_OP_COUNT, new PropertyModel<>(model, TraceViewDto.F_SHOW_REPO_OP_COUNT)));
        mainForm.add(new CheckBox(ID_SHOW_CONN_ID_OP_COUNT, new PropertyModel<>(model, TraceViewDto.F_SHOW_CONN_ID_OP_COUNT)));
        mainForm.add(new CheckBox(ID_SHOW_REPO_OP_TIME, new PropertyModel<>(model, TraceViewDto.F_SHOW_REPO_OP_TIME)));
        mainForm.add(new CheckBox(ID_SHOW_CONN_ID_OP_TIME, new PropertyModel<>(model, TraceViewDto.F_SHOW_CONN_ID_OP_TIME)));

        AjaxSubmitButton showButton = new AjaxSubmitButton(ID_SHOW, createStringResource("PageTraceView.button.show")) {
            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                visualize();
                target.add(PageTraceView.this);
            }
        };
        mainForm.add(showButton);
        Label resultLabel = new Label(ID_RESULT_LABEL, (IModel<String>) () -> {
            if (!model.getObject().isVisualized()) {
                return "";
            } else {
                return getString("PageTraceView.trace");
            }
        });
        mainForm.add(resultLabel);

        AceEditor resultText = new AceEditor(ID_RESULT_TEXT, new PropertyModel<>(model, TraceViewDto.F_VISUALIZED_TRACE));
        resultText.setReadonly(true);
        resultText.setResizeToMaxHeight(true);
        resultText.setMode(null);
        resultText.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().isVisualized();
            }
        });
        mainForm.add(resultText);
    }

    @NotNull
    private IModel<List<GenericTraceVisualizationType>> createStandardLevels() {
        return (IModel<List<GenericTraceVisualizationType>>) () ->
                Arrays.asList(
                        GenericTraceVisualizationType.ONE_LINE,
                        GenericTraceVisualizationType.BRIEF,
                        GenericTraceVisualizationType.FULL,
                        GenericTraceVisualizationType.HIDE);
    }

    @NotNull
    private IModel<List<GenericTraceVisualizationType>> createOthersLevels() {
        return (IModel<List<GenericTraceVisualizationType>>) () ->
                Arrays.asList(
                        GenericTraceVisualizationType.ONE_LINE,
                        GenericTraceVisualizationType.HIDE);
    }

    @NotNull
    private IModel<List<GenericTraceVisualizationType>> createClockworkLevels() {
        return (IModel<List<GenericTraceVisualizationType>>) () ->
                Arrays.asList(
                        GenericTraceVisualizationType.ONE_LINE,
                        GenericTraceVisualizationType.HIDE);
    }

    @NotNull
    private IModel<List<GenericTraceVisualizationType>> createMappingLevels() {
        return (IModel<List<GenericTraceVisualizationType>>) () ->
                Arrays.asList(
                        GenericTraceVisualizationType.ONE_LINE,
                        GenericTraceVisualizationType.BRIEF,
                        GenericTraceVisualizationType.DETAILED,
                        GenericTraceVisualizationType.FULL,
                        GenericTraceVisualizationType.HIDE);
    }

    private void visualize() {
        Task task = createSimpleTask(OP_VISUALIZE);
        OperationResult result = task.getResult();

        String oid = getPageParameters().get(PARAM_OBJECT_ID).toString();
        LOGGER.info("Visualizing trace from report output {}", oid);
        if (oid == null) {
            model.getObject().setVisualizedTrace("No report output OID specified");
            return;
        }

        try {
            // TODO consider using ReportManager to get trace input stream
            getSecurityEnforcer().authorize(
                    ModelAuthorizationAction.READ_TRACE.getUrl(), task, result);

            PrismObject<ReportDataType> reportOutput = getModelService().getObject(ReportDataType.class,
                    oid, null, task, result);
            String filePath = reportOutput.asObjectable().getFilePath();
            if (filePath != null) {
                String visualized = visualizeTrace(filePath);
                model.getObject().setVisualizedTrace(visualized);
            } else {
                throw new SchemaException("No trace file specified in " + reportOutput);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't visualize trace", t);
            result.recordFatalError(t);
            model.getObject().setVisualizedTrace("Couldn't visualize trace: " + t.getMessage() + "\n" + ExceptionUtil.printStackTrace(t));
        } finally {
            result.computeStatusIfUnknown();
            if (!result.isSuccess()) {
                showResult(result);
            }
        }
    }

    private String visualizeTrace(String filePath) throws IOException, SchemaException {
        createOpNodeList(filePath);
        TraceVisualizationInstructionsType instructions = createVisualizationInstructions();
        parsedOpNodeList.forEach(node -> node.applyVisualizationInstructions(instructions));
        TraceVisualizerRegistry registry = new TraceVisualizerRegistry(getPrismContext());
        TraceTreeVisualizer visualizer = new TraceTreeVisualizer(registry, parsedOpNodeList);
        return visualizer.visualize();
    }

    private void createOpNodeList(String filePath) throws IOException, SchemaException {
        if (parsedOpNodeList == null || !filePath.equals(parsedFilePath)) {
            PrismContext prismContext = getPrismContext();
            TraceParser parser = new TraceParser(prismContext);
            TracingOutputType parsed = parser.parse(new File(filePath));
            parsedOpNodeList = new OpNodeTreeBuilder(prismContext).build(parsed);
            parsedFilePath = filePath;
        }
    }

    private TraceVisualizationInstructionsType createVisualizationInstructions() {
        TraceViewDto dto = model.getObject();
        TraceVisualizationInstructionsType instructions = new TraceVisualizationInstructionsType(getPrismContext())
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.CLOCKWORK_EXECUTION)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getClockworkExecution())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.CLOCKWORK_CLICK)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getClockworkClick())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.MAPPING_EVALUATION)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getMappingEvaluation())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.FOCUS_LOAD)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getFocusLoad())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.PROJECTION_LOAD)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getProjectionLoad())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.FOCUS_CHANGE_EXECUTION)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getFocusChange())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginSelector()
                        .operationKind(OperationKindType.PROJECTION_CHANGE_EXECUTION)
                    .<TraceVisualizationInstructionType>end()
                    .beginVisualization()
                        .generic(dto.getProjectionChange())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginInstruction()
                    .beginVisualization()
                        .generic(dto.getOthers())
                    .<TraceVisualizationInstructionType>end()
                .<TraceVisualizationInstructionsType>end()
                .beginColumns()
                    .invocationId(dto.isShowInvocationId())
                    .durationBefore(dto.isShowDurationBefore())
                    .duration(dto.isShowDurationAfter())
                .end();

        List<String> countFor = instructions.getColumns().getCountFor();
        List<String> timeFor = instructions.getColumns().getTimeFor();
        if (dto.isShowRepoOpCount()) {
            countFor.add(PerformanceCategory.REPOSITORY_READ.name());
            countFor.add(PerformanceCategory.REPOSITORY_WRITE.name());
        }
        if (dto.isShowConnIdOpCount()) {
            countFor.add(PerformanceCategory.ICF_READ.name());
            countFor.add(PerformanceCategory.ICF_WRITE.name());
        }
        if (dto.isShowRepoOpTime()) {
            timeFor.add(PerformanceCategory.REPOSITORY.name());
        }
        if (dto.isShowConnIdOpTime()) {
            timeFor.add(PerformanceCategory.ICF.name());
        }
        return instructions;
    }
}
