/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import static com.evolveum.midpoint.gui.impl.page.admin.cases.component.CorrelationContextDto.F_OPTION_HEADERS;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "correlationContext")
@PanelInstance(identifier = "correlationContext",
        display = @PanelDisplay(label = "PageCase.correlationContextPanel", order = 1))
public class CorrelationContextPanel extends AbstractObjectMainPanel<CaseType, CaseDetailsModels> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationContextDto.class);

    private static final String ID_ACTION_CONTAINER = "actionContainer";
    private static final String ID_ACTIONS = "actions";
    private static final String ID_ACTION = "action";
    private static final String ID_OUTCOME_ICON = "outcomeIcon";
    private static final String ID_NAMES = "names";
    private static final String ID_NAME = "name";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_HEADERS = "headers";
    private static final String ID_HEADER = "header";
    private static final String ID_ACTION_LABEL = "actionLabel";
    private static final String ID_ROWS = "rows";
    private static final String ID_ATTR_NAME = "attrName";
    private static final String ID_COLUMN = "column";
    private static final String ID_MATCH_NAME = "matchName";
    private static final String ID_CONFIDENCES = "confidences";
    private static final String ID_CONFIDENCE = "confidence";
    private static final String ID_INFO = "explanation";

    private static final String OP_LOAD = CorrelationContextPanel.class.getName() + ".load";
    private static final String OP_DECIDE_CORRELATION = CorrelationContextPanel.class.getName() + ".decideCorrelation";

    // Move into properties
    private static final String TEXT_CREATE_NEW = "Create new";
    private static final String TEXT_CORRELATE = "Correlate";

    private static final String TEXT_MATCH_CONFIDENCE = "Match confidence";

    private final IModel<CaseWorkItemType> workItemModel;

    boolean caseWithConfidences = false;

    @SuppressWarnings("unused") // called by the framework
    public CorrelationContextPanel(String id, CaseDetailsModels model, ContainerPanelConfigurationType config) {
        this(id, model, null, config);
    }

    public CorrelationContextPanel(String id, CaseDetailsModels model, IModel<CaseWorkItemType> workItemModel, ContainerPanelConfigurationType config) {
        super(id, model, config);
        this.workItemModel = workItemModel;
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

        CaseType correlationCase = getObjectDetailsModels().getObjectType();
        WebMarkupContainer actionContainer = new WebMarkupContainer(ID_ACTION_CONTAINER);
        actionContainer.add(new VisibleBehaviour(() -> CaseTypeUtil.isClosed(correlationCase) || isPanelForItemWork()));
        add(actionContainer);

        ListView<CorrelationOptionDto> actions = new ListView<>(ID_ACTIONS,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {
                AjaxButton actionButton = new AjaxButton(ID_ACTION) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CaseWorkItemType workItem = workItemModel.getObject();
                        WorkItemId workItemId = WorkItemId.of(workItem);
                        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                                .outcome(item.getModelObject().getIdentifier())
                                .comment(workItem.getOutput() != null ? workItem.getOutput().getComment() : null);

                        PageBase page = getPageBase();
                        Task task = page.createSimpleTask(OP_DECIDE_CORRELATION);
                        OperationResult result = task.getResult();
                        try {
                            page.getCaseService().completeWorkItem(workItemId, output, task, result);
                            result.computeStatusIfUnknown();
                        } catch (Throwable e) {
                            result.recordFatalError("Cannot finish correlation process, " + e.getMessage(), e);
                        }

                        page.showResult(result);

                        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
                            target.add(page.getFeedbackPanel());
                        } else {
                            page.redirectBack();
                        }
                    }
                };

                actionButton.add(
                        new Label(ID_ACTION_LABEL,
                                item.getModelObject().isNewOwner() ? TEXT_CREATE_NEW : TEXT_CORRELATE));

                String outcome = correlationCase.getOutcome();
                actionButton.add(new VisibleBehaviour(() -> outcome == null));
                item.add(actionButton);

                WebMarkupContainer iconType = new WebMarkupContainer(ID_OUTCOME_ICON);
                iconType.setOutputMarkupId(true);
                iconType.add(new VisibleBehaviour(() -> outcome != null && item.getModelObject().matches(outcome)));
                item.add(iconType);
            }
        };
        actionContainer.add(actions);

        ListView<CorrelationOptionDto> referenceIds = new ListView<>(ID_NAMES,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {
                // A full-object reference to the candidate owner
                ReadOnlyModel<ObjectReferenceType> referenceModel = new ReadOnlyModel<>(
                        () -> {
                            CorrelationOptionDto optionDto = item.getModelObject();
                            if (!optionDto.isNewOwner()) {
                                return ObjectTypeUtil.createObjectRefWithFullObject(
                                        optionDto.getObject());
                            } else {
                                // GUI cannot currently open object that does not exist in the repository.
                                return null;
                            }
                        }
                );
                item.add(
                        new LinkedReferencePanel<>(ID_NAME, referenceModel));
            }
        };
        add(referenceIds);

        Label matchLabel = new Label(ID_MATCH_NAME, TEXT_MATCH_CONFIDENCE);
        add(matchLabel.setVisible(caseWithConfidences));

        ListView<CorrelationOptionDto> matchConfidences = new ListView<>(ID_CONFIDENCES,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {

                if (item.getModelObject().getConfidence() != null) {
                    caseWithConfidences = true;
                }

                item.add(new Label(ID_CONFIDENCE, item.getModel().getObject().getConfidence())
                        .setVisible(item.getModelObject().getConfidence() != null));

                item.add(WebComponentUtil.createHelp(ID_INFO)
                        .setVisible(item.getModelObject().getConfidence() != null));

                matchLabel.setVisible(caseWithConfidences);
                this.setVisible(caseWithConfidences);
            }
        };

        add(matchConfidences);

        ListView<CorrelationProperty> rows = new ListView<>(ID_ROWS,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_PROPERTIES)) {

            @Override
            protected void populateItem(ListItem<CorrelationProperty> item) {
                // First column contains the property name
                item.add(
                        new Label(ID_ATTR_NAME, new StringResourceModel("${displayName}", item.getModel())));

                // Here are columns for values for individual options
                item.add(
                        createColumnsForPropertyRow(correlationCtxModel, item));
            }
        };
        add(rows);
    }

    private boolean isPanelForItemWork() {
        return workItemModel != null && workItemModel.getObject() != null;
    }

    private IModel<CorrelationContextDto> createCorrelationContextModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected CorrelationContextDto load() {
                CaseType aCase = getObjectDetailsModels().getObjectType();
                CaseCorrelationContextType correlationContext = aCase.getCorrelationContext();
                if (correlationContext == null || CorrelationCaseUtil.getOwnerOptions(aCase) == null) {
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
            }
        };
    }

    private ListView<CorrelationOptionDto> createColumnsForPropertyRow(
            IModel<CorrelationContextDto> contextModel, ListItem<CorrelationProperty> rowItem) {

        return new ListView<>(ID_COLUMNS, new PropertyModel<>(contextModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {
            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> columnItem) {
                CorrelationContextDto contextDto = contextModel.getObject();
                CorrelationOptionDto optionDto = columnItem.getModelObject();
                CorrelationProperty correlationProperty = rowItem.getModelObject();

                CorrelationPropertyValues valuesForOption = optionDto.getPropertyValues(correlationProperty);
                Label label = new Label(ID_COLUMN, valuesForOption.format());

                CorrelationOptionDto referenceOption = contextDto.getNewOwnerOption();
                if (referenceOption != null && !optionDto.isNewOwner()) {
                    CorrelationPropertyValues referenceValues = referenceOption.getPropertyValues(correlationProperty);
                    Match match = referenceValues.match(valuesForOption);
                    label.add(
                            AttributeAppender.append("class", match.getCss()));
                }
                columnItem.add(label);
            }
        };
    }
}
