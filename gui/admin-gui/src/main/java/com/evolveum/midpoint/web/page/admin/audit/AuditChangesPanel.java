/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.audit;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.ChangesPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AuditChangesPanel extends ChangesPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AuditChangesPanel.class);

    private static final String ID_EXECUTION_RESULT = "executionResult";
    private static final String ID_FULL_RESULT_LINK = "fullResultLink";
    private static final String ID_OBJECT_NAME = "objectName";
    private static final String ID_RESOURCE_NAME = "resourceName";

    private final IModel<ObjectDeltaOperationType> deltaOperationModel;

    public AuditChangesPanel(String id, IModel<ObjectDeltaOperationType> deltaOperationModel, PageBase page) {
        super(id, new VisualizationModel(page, deltaOperationModel));

        this.deltaOperationModel = deltaOperationModel;

        setShowOperationalItems(true);

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer body = getBody();

        add(AttributeModifier.append("class", (IModel<String>) this::getBoxCssClass));

        Label executionResult = new Label(ID_EXECUTION_RESULT, new PropertyModel<>(deltaOperationModel, "executionResult.status"));
        body.add(executionResult);

        AjaxLink<Void> showFullResultsLink = new AjaxLink<>(ID_FULL_RESULT_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showFullResultsPerformed(target);
            }
        };
        showFullResultsLink.add(new VisibleBehaviour(this::isShowFullResultVisible));
        body.add(showFullResultsLink);

        Label resourceName = new Label(ID_RESOURCE_NAME,
                new PropertyModel<>(deltaOperationModel, ObjectDeltaOperationType.F_RESOURCE_NAME.getLocalPart()));
        resourceName.add(new VisibleBehaviour(() -> deltaOperationModel.getObject().getResourceName() != null));
        body.add(resourceName);

        Label objectName = new Label(ID_OBJECT_NAME,
                new PropertyModel<>(deltaOperationModel, ObjectDeltaOperationType.F_OBJECT_NAME.getLocalPart()));
        body.add(objectName);
    }

    private void showFullResultsPerformed(AjaxRequestTarget target) {
        OperationResultPopupPanel operationResultPopupPanel = new OperationResultPopupPanel(
                getPageBase().getMainPopupBodyId(), createOperationResultModel());
        getPageBase().showMainPopup(operationResultPopupPanel, target);
    }

    private boolean isShowFullResultVisible() {
        return isExecutionEventStage()
                && !WebComponentUtil.isSuccessOrHandledError(deltaOperationModel.getObject().getExecutionResult());
    }

    private boolean isExecutionEventStage() {
        return deltaOperationModel.getObject() != null && deltaOperationModel.getObject().getExecutionResult() != null;
    }

    private IModel<OperationResult> createOperationResultModel() {
        return () -> {
            if (deltaOperationModel == null) {
                return null;
            }
            OperationResultType executionResult = deltaOperationModel.getObject().getExecutionResult();
            if (executionResult == null) {
                return null;
            }
            return OperationResult.createOperationResult(executionResult);
        };
    }

    private String getBoxCssClass() {
        ObjectDeltaOperationType deltaOperation = deltaOperationModel.getObject();
        if (deltaOperation == null || deltaOperation.getExecutionResult() == null) {
            return "card-primary";
        }

        OperationResultStatusType status = deltaOperation.getExecutionResult().getStatus();
        if (status == null) {
            return "card-primary";
        }

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

    private static class VisualizationModel extends LoadableModel<List<VisualizationDto>> {

        private final PageBase page;
        private final IModel<ObjectDeltaOperationType> model;

        public VisualizationModel(PageBase page, IModel<ObjectDeltaOperationType> model) {
            super(false);
            this.page = page;
            this.model = model;
        }

        @Override
        protected List<VisualizationDto> load() {
            try {
                return loadVisualizationForDelta();
            } catch (SchemaException | ExpressionEvaluationException e) {
                OperationResult result = new OperationResult(AuditChangesPanel.class.getName() + ".loadSceneForDelta");
                result.recordFatalError(LocalizationUtil.translate("AuditChangesPanel.message.fetchOrVisualize.fatalError",
                        new Object[] { e.getMessage() }), e);
                page.showResult(result);
                throw page.redirectBackViaRestartResponseException();
            }
        }

        private List<VisualizationDto> loadVisualizationForDelta() throws SchemaException, ExpressionEvaluationException {
            Visualization visualization;

            ObjectDelta<? extends ObjectType> delta;
            ObjectDeltaType deltaType = model.getObject().getObjectDelta();
            try {
                delta = DeltaConvertor.createObjectDelta(deltaType, page.getPrismContext());
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "SchemaException while converting delta:\n{}", e, deltaType);
                throw e;
            }

            try {
                Task task = page.createSimpleTask("visualized delta");
                visualization = page.getModelInteractionService().visualizeDelta(delta, true,
                        false, task, task.getResult());
            } catch (SchemaException | ExpressionEvaluationException e) {
                LoggingUtils.logException(LOGGER, "SchemaException while visualizing delta:\n{}", e, DebugUtil.debugDump(delta));
                throw e;
            }

            return Collections.singletonList(new VisualizationDto(visualization));
        }
    }
}
