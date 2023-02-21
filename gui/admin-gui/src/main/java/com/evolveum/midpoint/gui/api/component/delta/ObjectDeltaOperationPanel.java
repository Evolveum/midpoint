/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.delta;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ObjectDeltaOperationPanel extends BasePanel<ObjectDeltaOperationType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaOperationPanel.class);

    private static final String ID_PARAMETERS_DELTA = "delta";
    private static final String ID_PARAMETERS_EXECUTION_RESULT = "executionResult";
    private static final String ID_PARAMETERS_FULL_RESULT_LINK = "fullResultLink";
    private static final String ID_PARAMETERS_OBJECT_NAME = "objectName";
    private static final String ID_PARAMETERS_RESOURCE_NAME = "resourceName";

    private static final String ID_DELTA_PANEL = "deltaPanel";
    private static final String ID_OBJECT_DELTA_OPERATION_MARKUP = "objectDeltaOperationMarkup";
    PageBase parentPage;

    public ObjectDeltaOperationPanel(String id, IModel<ObjectDeltaOperationType> model, PageBase parentPage) {
        super(id, model);
        this.parentPage = parentPage;
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer objectDeltaOperationMarkup = new WebMarkupContainer(ID_OBJECT_DELTA_OPERATION_MARKUP);
        objectDeltaOperationMarkup.setOutputMarkupId(true);

        objectDeltaOperationMarkup.add(AttributeModifier.append("class", (IModel<String>) this::getBoxCssClass));
        add(objectDeltaOperationMarkup);

        Label executionResult = new Label(ID_PARAMETERS_EXECUTION_RESULT,
                new PropertyModel<>(getModel(), "executionResult.status"));
        executionResult.setOutputMarkupId(true);
        objectDeltaOperationMarkup.add(executionResult);

        AjaxButton showFullResultsLink = new AjaxButton(ID_PARAMETERS_FULL_RESULT_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showFullResultsPerformed(target);
            }

            @Override
            public IModel<?> getBody() {
                return getPageBase().createStringResource("ObjectDeltaOperationResult.showFullResult");
            }
        };
        showFullResultsLink.setOutputMarkupId(true);
        showFullResultsLink.add(AttributeAppender.append("style", "cursor: pointer;"));
        showFullResultsLink.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getExecutionResult() != null));
        objectDeltaOperationMarkup.add(showFullResultsLink);

        Label resourceName = new Label(ID_PARAMETERS_RESOURCE_NAME,
                new PropertyModel<>(getModel(), ObjectDeltaOperationType.F_RESOURCE_NAME.getLocalPart()));
        resourceName.setOutputMarkupId(true);
        objectDeltaOperationMarkup.add(resourceName);

        Label objectName = new Label(ID_PARAMETERS_OBJECT_NAME,
                new PropertyModel<>(getModel(), ObjectDeltaOperationType.F_OBJECT_NAME.getLocalPart()));
        objectName.setOutputMarkupId(true);
        objectDeltaOperationMarkup.add(objectName);
        final VisualizationDto visualizationDto;
        try {
            visualizationDto = loadVisualizationForDelta();
        } catch (SchemaException | ExpressionEvaluationException e) {
            OperationResult result = new OperationResult(ObjectDeltaOperationPanel.class.getName() + ".loadSceneForDelta");
            result.recordFatalError(createStringResource("ObjectDeltaOperationPanel.message.fetchOrVisualize.fatalError", e.getMessage()).getString(), e);
            parentPage.showResult(result);
            throw parentPage.redirectBackViaRestartResponseException();
        }
        IModel<VisualizationDto> deltaModel = new IModel<>() {
            private static final long serialVersionUID = 1L;

            public VisualizationDto getObject() {
                return visualizationDto;
            }

        };
        VisualizationPanel deltaPanel = new VisualizationPanel(ID_DELTA_PANEL, deltaModel, true, true) {
            @Override
            public void headerOnClickPerformed(AjaxRequestTarget target, IModel<VisualizationDto> model) {
                super.headerOnClickPerformed(target, model);
//                model.getObject().setMinimized(!model.getObject().isMinimized());
                target.add(ObjectDeltaOperationPanel.this);
            }
        };
        deltaPanel.setOutputMarkupId(true);
        objectDeltaOperationMarkup.add(deltaPanel);

    }

    private String getBoxCssClass() {
        if (getModel().getObject() == null) {
            return "card-primary";
        }

        if (getModel().getObject().getExecutionResult() == null) {
            return "card-primary";
        }

        if (getModel().getObject().getExecutionResult().getStatus() == null) {
            return "card-primary";
        }

        OperationResultStatusType status = getModel().getObject().getExecutionResult().getStatus();
        switch (status) {
            case PARTIAL_ERROR:
            case FATAL_ERROR:
                return "card-danger";
            case WARNING:
            case UNKNOWN:
            case HANDLED_ERROR:
                return "card-warning";
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                return "card-primary";
            case SUCCESS:
                return "card-success";

        }
        return "card-primary";

    }

    private VisualizationDto loadVisualizationForDelta() throws SchemaException, ExpressionEvaluationException {
        Visualization visualization;

        ObjectDelta<? extends ObjectType> delta;
        ObjectDeltaType deltaType = getModel().getObject().getObjectDelta();
        try {
            delta = DeltaConvertor.createObjectDelta(deltaType,
                    parentPage.getPrismContext());
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "SchemaException while converting delta:\n{}", e, deltaType);
            throw e;
        }
        try {
            visualization = parentPage.getModelInteractionService().visualizeDelta(delta, true, getIncludeOriginalObject(),
                    parentPage.createSimpleTask(ID_PARAMETERS_DELTA),
                    new OperationResult(ID_PARAMETERS_DELTA));
        } catch (SchemaException | ExpressionEvaluationException e) {
            LoggingUtils.logException(LOGGER, "SchemaException while visualizing delta:\n{}",
                    e, DebugUtil.debugDump(delta));
            throw e;
        }
        VisualizationDto visualizationDto = new VisualizationDto(visualization);
        visualizationDto.setMinimized(true);
        return visualizationDto;

    }

    public boolean getIncludeOriginalObject() {
        return true;
    }

    private void showFullResultsPerformed(AjaxRequestTarget target) {
        OperationResultPopupPanel operationResultPopupPanel = new OperationResultPopupPanel(
                getPageBase().getMainPopupBodyId(), createOperationResultModel());
        getPageBase().showMainPopup(operationResultPopupPanel, target);
    }

    private IModel<OperationResult> createOperationResultModel() {
        return () -> {
            if (getModelObject() == null) {
                return null;
            }
            OperationResultType executionResult = getModelObject().getExecutionResult();
            if (executionResult == null) {
                return null;
            }
            return OperationResult.createOperationResult(executionResult);
        };
    }
}
