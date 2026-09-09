/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;

public class OperationResultCollapsedItemPanel extends BasePanel<OperationResultCollapsedItem> {

    public enum ResultType {
        ERROR("fa fa-exclamation-circle text-danger", "callout callout-danger"),
        WARNING("fa fa-exclamation-triangle text-warning", "callout callout-warning"),
        UNKNOWN("fa fa-question-circle text-info", "callout callout-info");

        final String css;
        final String icon;

        ResultType(String icon, String css) {
            this.css = css;
            this.icon = icon;
        }

        public String getCss() {
            return css;
        }

        public String getIcon() {
            return icon;
        }
    }

    private static final String ID_RESULT = "result";
    private static final String ID_EXCEPTION = "exception";

    private final WizardModelWithParentSteps wizardModel;

    public OperationResultCollapsedItemPanel(String id, IModel<OperationResultCollapsedItem> model, WizardModelWithParentSteps wizardModel) {
        super(id, model);
        this.wizardModel = wizardModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createResultList());
    }

    private @NotNull ListView<OperationResultWrapper> createResultList() {
        ListView<OperationResultWrapper> results =
                new ListView<>(ID_RESULT, () -> getModelObject().getResults()) {

                    @Override
                    protected void populateItem(ListItem<OperationResultWrapper> item) {
                        item.add(new ProcessedExceptionPanel(
                                ID_EXCEPTION, item.getModel(), wizardModel));
                    }
                };
        results.setOutputMarkupId(true);
        return results;
    }
}
