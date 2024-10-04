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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.io.Serial;
import java.util.List;

/**
 * @author skublik
 */

public class PasswordLimitationsPanel extends BasePanel<List<StringLimitationResult>> {

    private static final String ID_VALIDATION_CONTAINER = "validationContainer";
    private static final String ID_VALIDATION_ITEMS_PARENT = "validationItemsParent";
    private static final String ID_VALIDATION_ITEMS = "validationItems";
    private static final String ID_VALIDATION_ITEM = "validationItem";

    public PasswordLimitationsPanel(String id, LoadableDetachableModel<List<StringLimitationResult>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer validationContainer = new WebMarkupContainer(ID_VALIDATION_CONTAINER) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getModelObject().isEmpty();
            }
        };
        validationContainer.setOutputMarkupId(true);
        validationContainer.setOutputMarkupPlaceholderTag(true);
        add(validationContainer);

        WebMarkupContainer validationItemsParent = new WebMarkupContainer(ID_VALIDATION_ITEMS_PARENT);
        validationItemsParent.setOutputMarkupId(true);
        validationItemsParent.add(AttributeAppender.append("class", showInTwoColumns() ? "d-flex flex-wrap flex-row" : ""));
        validationContainer.add(validationItemsParent);

        ListView<StringLimitationResult> validationItems = new ListView<>(ID_VALIDATION_ITEMS, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<StringLimitationResult> item) {
                StringLimitationPanel limitationPanel = new StringLimitationPanel(ID_VALIDATION_ITEM, item.getModel());
                limitationPanel.setOutputMarkupId(true);
                item.add(limitationPanel);
                item.add(AttributeAppender.append("class", showInTwoColumns() ? "col-xxl-6 col-xl-12" : ""));
                item.add(AttributeModifier.append("class", (IModel<String>) () -> Boolean.TRUE.equals(item.getModelObject().isSuccess()) ? " text-success" : " text-danger"));
            }
        };
        validationItems.setOutputMarkupId(true);
        validationItemsParent.add(validationItems);
    }

    public void refreshItems(AjaxRequestTarget target){
        target.add(PasswordLimitationsPanel.this.get(ID_VALIDATION_CONTAINER));
    }

    protected boolean showInTwoColumns() {
        return false;
    }
}
