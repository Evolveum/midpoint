/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author skublik
 */

public class PasswordLimitationsPanel extends BasePanel<List<StringLimitationResult>> {

    private static final String ID_VALIDATION_CONTAINER = "validationContainer";
    private static final String ID_VALIDATION_PARENT_ITEMS = "validationParentItems";
    private static final String ID_VALIDATION_ITEMS = "validationItems";
    private static final String ID_VALIDATION_ITEM = "validationItem";

    public PasswordLimitationsPanel(String id, IModel<List<StringLimitationResult>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final WebMarkupContainer validationContainer = new WebMarkupContainer(ID_VALIDATION_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getModelObject().isEmpty();
            }
        };
        validationContainer.setOutputMarkupId(true);
        add(validationContainer);

        final WebMarkupContainer validationParentContainer = new WebMarkupContainer(ID_VALIDATION_PARENT_ITEMS) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getModelObject().isEmpty();
            }
        };
        validationParentContainer.setOutputMarkupId(true);
        validationContainer.add(validationParentContainer);

        ListView<StringLimitationResult> validationItems = new ListView<>(ID_VALIDATION_ITEMS, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<StringLimitationResult> item) {
                StringLimitationPanel limitationPanel = new StringLimitationPanel(ID_VALIDATION_ITEM, item.getModel());
                limitationPanel.setOutputMarkupId(true);
                item.add(limitationPanel);
                item.add(AttributeModifier.append("class", new IModel<String>() {
                    @Override
                    public String getObject() {
                        return Boolean.TRUE.equals(item.getModelObject().isSuccess()) ? " text-success" : " text-danger";
                    }
                }));
            }
        };
        validationItems.setOutputMarkupId(true);
        validationParentContainer.add(validationItems);
    }

    public void refreshItems(AjaxRequestTarget target){
        target.add(get(createComponentPath(ID_VALIDATION_CONTAINER, ID_VALIDATION_PARENT_ITEMS)));
    }
}
