/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import static com.evolveum.midpoint.gui.impl.page.admin.cases.component.CorrelationContextDto.F_CORRELATION_OPTIONS;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
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
    private static final String ID_CANDIDATE_REFERENCES = "candidateReferences";
    private static final String ID_CANDIDATE_REFERENCE = "candidateReference";
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
    private static final String ID_EXPLANATION = "explanation";

    private static final String OP_LOAD = CorrelationContextPanel.class.getName() + ".load";
    private static final String OP_DECIDE_CORRELATION = CorrelationContextPanel.class.getName() + ".decideCorrelation";

    private final IModel<CaseWorkItemType> workItemModel;

    @SuppressWarnings("unused") // called by the framework
    public CorrelationContextPanel(String id, CaseDetailsModels model, ContainerPanelConfigurationType config) {
        this(id, model, null, config);
    }

    public CorrelationContextPanel(
            String id, CaseDetailsModels model, IModel<CaseWorkItemType> workItemModel, ContainerPanelConfigurationType config) {
        super(id, model, config);
        this.workItemModel = workItemModel;
    }

    @Override
    protected void initLayout() {
        IModel<CorrelationContextDto> correlationCtxModel = createCorrelationContextModel();
        CaseType correlationCase = getObjectDetailsModels().getObjectType();

        // 1. The header row contain the "object being correlated" and "correlation candidate XXX" labels
        ListView<CorrelationOptionDto> headers =
                new ListView<>(ID_HEADERS, new PropertyModel<>(correlationCtxModel, F_CORRELATION_OPTIONS)) {
                    @Override
                    protected void populateItem(ListItem<CorrelationOptionDto> item) {
                        item.add(new Label(ID_HEADER, () -> {
                            if (item.getModelObject() instanceof CorrelationOptionDto.NewOwner) {
                                return LocalizationUtil.translate("CorrelationContextPanel.objectBeingCorrelated");
                            } else {
                                return LocalizationUtil.translate("CorrelationContextPanel.correlationCandidate",
                                        new Object[] { item.getIndex() });
                            }
                        }));
                    }
                };
        add(headers);

        // 2. The optional "action" row contains buttons to use particular candidate, or to create a new owner.
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

                // Correlation candidate or "new owner" option.
                CorrelationOptionDto optionDto = item.getModelObject();

                actionButton.add(
                        new Label(ID_ACTION_LABEL,
                                createStringResource(
                                        optionDto.isNewOwner() ?
                                                "CorrelationContextPanel.createNew" : "CorrelationContextPanel.correlate")));

                String outcome = correlationCase.getOutcome();
                actionButton.add(new VisibleBehaviour(() -> outcome == null));
                item.add(actionButton);

                WebMarkupContainer iconType = new WebMarkupContainer(ID_OUTCOME_ICON);
                iconType.setOutputMarkupId(true);
                iconType.add(new VisibleBehaviour(() -> outcome != null && optionDto.matches(outcome)));
                item.add(iconType);
            }
        };
        actionContainer.add(actions);

        // 3. "Candidate references" row contains the names (and clickable links) to individual candidates
        ListView<CorrelationOptionDto> candidateReferences = new ListView<>(ID_CANDIDATE_REFERENCES,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {
                // A full-object reference to the candidate owner
                IModel<ObjectReferenceType> candidateReferenceModel = () -> {
                    if (item.getModelObject() instanceof CorrelationOptionDto.Candidate candidate) {
                        return ObjectTypeUtil.createObjectRefWithFullObject(candidate.getObject());
                    } else {
                        return null; // GUI cannot currently open object that does not exist in the repository.
                    }
                };
                item.add(
                        new LinkedReferencePanel<>(ID_CANDIDATE_REFERENCE, candidateReferenceModel));
            }
        };
        add(candidateReferences);

        // 4. "Match confidence" row contains the match confidence values for individual candidates
        // TODO we should make visible/invisible the whole row, not the individual components
        Label matchLabel = new Label(ID_MATCH_NAME, createStringResource("CorrelationContextPanel.matchConfidence"));
        matchLabel.add(new VisibleBehaviour(() -> correlationCtxModel.getObject().hasConfidences()));
        add(matchLabel);

        ListView<CorrelationOptionDto> matchConfidences = new ListView<>(ID_CONFIDENCES,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> item) {

                CorrelationOptionDto optionDto = item.getModelObject();

                String confidence = optionDto.getCandidateConfidenceString();
                Label confidenceLabel = new Label(ID_CONFIDENCE, confidence);
                confidenceLabel.setVisible(confidence != null);
                item.add(confidenceLabel);

                String explanation = optionDto.getCandidateExplanation();
                Component explanationLabel = WebComponentUtil.createHelp(ID_EXPLANATION);
                explanationLabel.add(AttributeModifier.replace("title", explanation));
                explanationLabel.setVisible(explanation != null);
                item.add(explanationLabel);
            }
        };
        add(matchConfidences);

        // 5. A set of rows for individual correlation properties (given name, family name, and so on).
        ListView<CorrelationPropertyDefinition> rows = new ListView<>(ID_ROWS,
                new PropertyModel<>(correlationCtxModel, CorrelationContextDto.F_CORRELATION_PROPERTIES_DEFINITIONS)) {

            @Override
            protected void populateItem(ListItem<CorrelationPropertyDefinition> item) {
                // First column contains the property name
                item.add(
                        new Label(ID_ATTR_NAME,
                                () -> LocalizationUtil.translate(
                                        item.getModelObject().getDisplayName())));

                // Here are columns for values for individual options
                item.add(
                        createColumnsForCorrelationPropertyRow(correlationCtxModel, item));
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

    private ListView<CorrelationOptionDto> createColumnsForCorrelationPropertyRow(
            IModel<CorrelationContextDto> contextModel, ListItem<CorrelationPropertyDefinition> rowItem) {

        return new ListView<>(ID_COLUMNS, new PropertyModel<>(contextModel, CorrelationContextDto.F_CORRELATION_OPTIONS)) {
            @Override
            protected void populateItem(ListItem<CorrelationOptionDto> columnItem) {

                // This is the column = option = the specific candidate (or "reference" object - the one that is being matched).
                CorrelationOptionDto optionDto = columnItem.getModelObject();

                // This is the row = the correlation property in question (given name, family name, and so on).
                CorrelationPropertyDefinition correlationPropertyDef = rowItem.getModelObject();

                // Provide the values: either for a candidate or for the reference (object being matched).
                CorrelationPropertyValues values = optionDto.getPropertyValues(correlationPropertyDef);
                Label valuesLabel = new Label(ID_COLUMN, values.format());

                // Colorize the field
                if (optionDto instanceof CorrelationOptionDto.Candidate candidate) {
                    MatchVisualizationStyle matchVisualizationStyle;
                    var propertyValuesDescription = candidate.getPropertyValuesDescription(correlationPropertyDef);
                    if (propertyValuesDescription != null) {
                        matchVisualizationStyle = MatchVisualizationStyle.forMatch(propertyValuesDescription.getMatch());
                    } else {
                        matchVisualizationStyle = MatchVisualizationStyle.NOT_APPLICABLE;
                    }
                    valuesLabel.add(
                            AttributeAppender.append("class", matchVisualizationStyle.getCss()));
                }

                columnItem.add(valuesLabel);
            }
        };
    }
}
