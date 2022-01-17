/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "correlationContext")
@PanelInstance(identifier = "correlationContext",
        display = @PanelDisplay(label = "PageCase.correlationContextPanel", order = 40))
public class CorrelationContextPanel extends AbstractObjectMainPanel<CaseType, CaseDetailsModels> {

    public CorrelationContextPanel(String id, CaseDetailsModels model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        IModel<CorrelationContextDto> correlationCtxModel = createCorrelationContextModel();

        ListView<String> headers = new ListView<>("headers", new PropertyModel<>(correlationCtxModel, "matchHeaders")) {

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label("header", item.getModel()));
            }
        };
        add(headers);

        ListView<PotentialMatchDto> actions = new ListView<>("actions", new PropertyModel<>(correlationCtxModel, "potentialMatches")) {

            @Override
            protected void populateItem(ListItem<PotentialMatchDto> item) {
                AjaxButton actionButton = new AjaxButton("action") {

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

                actionButton.add(new Label("actionLabel", item.getModelObject().isOrigin() ? "Create new account" : "Use this account"));
                item.add(actionButton);
            }
        };
        add(actions);

        ListView<ItemPath> a = new ListView<>("rows", new PropertyModel<>(correlationCtxModel, "attributes")) {

            @Override
            protected void populateItem(ListItem<ItemPath> item) {
                item.add(new Label("attrName", item.getModel()));
                item.add(createAttributeValueColumns(correlationCtxModel, item));
            }
        };
        add(a);

    }

    private IModel<CorrelationContextDto> createCorrelationContextModel() {
        return new ReadOnlyModel<>(() -> {
            CorrelationContextType correlationContext = getObjectDetailsModels().getObjectType().getCorrelationContext();
            if (correlationContext == null) {
                return null;
            }

            AbstractCorrelationContextType ctx = correlationContext.getCorrelatorPart();

            IdMatchCorrelationContextType idMatchCorrelationContext;
            if (ctx instanceof IdMatchCorrelationContextType) {
                idMatchCorrelationContext = (IdMatchCorrelationContextType) ctx;
            } else {
                return null;
            }

            return new CorrelationContextDto(idMatchCorrelationContext);
        });
    }

    private ListView<PotentialMatchDto> createAttributeValueColumns(IModel<CorrelationContextDto> modelObject, ListItem<ItemPath> item) {
        return new ListView<>("columns", new PropertyModel<>(modelObject, "potentialMatches")) {
            @Override
            protected void populateItem(ListItem<PotentialMatchDto> columnItem) {
                Serializable value = columnItem.getModelObject().getAttributeValue(item.getModelObject());
                boolean equals = modelObject.getObject().match(value, item.getModelObject());
                Label label = new Label("column", value != null ? value : "");
                if (!columnItem.getModelObject().isOrigin()) {
                    label.add(AttributeAppender.append("class", equals ? "bg-green disabled color-palette" : "bg-red disabled color-palette"));
                }
                columnItem.add(label);
            }
        };
    }

}
