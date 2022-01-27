/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.cases.component.CorrelationContextDto.F_OPTION_HEADERS;

@PanelType(name = "correlationContext")
@PanelInstance(identifier = "correlationContext",
        display = @PanelDisplay(label = "PageCase.correlationContextPanel", order = 40))
public class CorrelationContextPanel extends AbstractObjectMainPanel<CaseType, CaseDetailsModels> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationContextDto.class);

    private static final String ID_ACTIONS = "actions";
    private static final String ID_ACTION = "action";
    private static final String ID_NAMES = "names";
    private static final String ID_NAME = "name";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_HEADERS = "headers";
    private static final String ID_HEADER = "header";
    private static final String ID_ACTION_LABEL = "actionLabel";
    private static final String ID_ROWS = "rows";
    private static final String ID_ATTR_NAME = "attrName";
    private static final String ID_COLUMN = "column";

    private static final String OP_LOAD = CorrelationContextPanel.class.getName() + ".load";

    @SuppressWarnings("unused") // called by the framework
    public CorrelationContextPanel(String id, CaseDetailsModels model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        IModel<CorrelationContextDto> correlationCtxModel = createCorrelationContextModel();

        ListView<String> headers = new ListView<>(ID_HEADERS, new PropertyModel<>(correlationCtxModel, F_OPTION_HEADERS)) {

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_HEADER, item.getModel()));
            }
        };
        add(headers);

        ListView<CorrelationOptionDto> actions = new ListView<>(ID_ACTIONS,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {
                AjaxButton actionButton = new AjaxButton(ID_ACTION) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CaseType correlationCase = getObjectDetailsModels().getObjectType();
                        WorkItemId workItemId = WorkItemId.of(correlationCase.getWorkItem().get(0));
                        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                                .outcome(item.getModelObject().getUri());

                        Task task = getPageBase().createSimpleTask("DecideCorrelation");
                        OperationResult result = task.getResult();
                        try {
                            getPageBase().getWorkflowService().completeWorkItem(workItemId, output, task, result);
                            result.computeStatusIfUnknown();
                        } catch (Throwable e) {
                            result.recordFatalError("Cannot finish correlation process, " + e.getMessage(), e);
                        }
                        getPageBase().showResult(result);
                        target.add(getPageBase().getFeedbackPanel());
                    }
                };

                actionButton.add(
                        new Label(ID_ACTION_LABEL,
                                item.getModelObject().isNewOwner() ? "Create new owner" : "Use this owner"));

                item.add(actionButton);
            }
        };
        add(actions);

        ListView<CorrelationOptionDto> referenceIds = new ListView<>(ID_NAMES,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {
                item.add(new Label(ID_NAME, item.getModelObject().getReferenceId()));
            }
        };
        add(referenceIds);

        ListView<CorrelationPropertyDefinition> rows = new ListView<>(ID_ROWS,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_PROPERTIES)) {

            @Override
            protected void populateItem(ListItem<CorrelationPropertyDefinition> item) {
                // First column contains the property name
                item.add(
                        new Label(ID_ATTR_NAME,
                                new PropertyModel<>(
                                        item.getModel(), CorrelationPropertyDefinition.F_DISPLAY_NAME)));

                // Here are columns for values for individual options
                item.add(
                        createColumnsForPropertyRow(correlationCtxModel, item));
            }
        };
        add(rows);
    }

    private IModel<CorrelationContextDto> createCorrelationContextModel() {
        return new ReadOnlyModel<>(() -> {
            CaseType aCase = getObjectDetailsModels().getObjectType();
            CorrelationContextType correlationContext = aCase.getCorrelationContext();
            if (correlationContext == null || correlationContext.getPotentialOwners() == null) {
                return null;
            }

            Task task = getPageBase().createSimpleTask(OP_LOAD);
            OperationResult result = task.getResult();
            try {
                return new CorrelationContextDto(aCase, getPageBase(), task, result);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load correlation context for {}", t, aCase);
                return null;
            } finally {
                result.computeStatusIfUnknown();
                getPageBase().showResult(result, false);
            }
        });
    }

    private ListView<CorrelationOptionDto> createColumnsForPropertyRow(
            IModel<CorrelationContextDto> contextModel, ListItem<CorrelationPropertyDefinition> rowItem) {

        return new ListView<>(ID_COLUMNS, new PropertyModel<>(contextModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {
            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> columnItem) {
                CorrelationContextDto contextDto = contextModel.getObject();
                CorrelationOptionDto optionDto = columnItem.getModelObject();
                CorrelationPropertyDefinition propertyDefinition = rowItem.getModelObject();

                CorrelationPropertyValues valuesForOption = optionDto.getPropertyValues(propertyDefinition);
                Label label = new Label(ID_COLUMN, valuesForOption.format());

                CorrelationOptionDto referenceOption = contextDto.getNewOwnerOption();
                if (referenceOption != null && !optionDto.isNewOwner()) {
                    CorrelationPropertyValues referenceValues = referenceOption.getPropertyValues(propertyDefinition);
                    Match match = referenceValues.match(valuesForOption);
                    label.add(
                            AttributeAppender.append("class", match.getCss()));
                }
                columnItem.add(label);
            }
        };
    }
}
