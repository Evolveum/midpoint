/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class SimpleCustomSearchPanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_TEXT_FIELD = "textField";
    private static final String ID_SEARCH_BUTTON = "searchButton";

    public SimpleCustomSearchPanel(String id, IModel<String> searchModel) {
        super(id, searchModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        TextField<String> searchInput = new TextField<>(ID_TEXT_FIELD, getModel());
        searchInput.setOutputMarkupId(true);

        searchInput.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateSearchModel(target, searchInput.getModelObject());
            }
        });
        container.add(searchInput);

        AjaxIconButton submitSearch = buildSearchButton(searchInput);
        container.add(submitSearch);
    }

    private @NotNull AjaxIconButton buildSearchButton(TextField<String> searchInput) {
        AjaxIconButton submitSearch = new AjaxIconButton(
                ID_SEARCH_BUTTON, Model.of(GuiStyleConstants.CLASS_ICON_SEARCH),
                createStringResource("SimpleCustomSearchPanel.search")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                updateSearchModel(target, searchInput.getModelObject());
                searchPerformed(target);
            }
        };
        submitSearch.showTitleAsLabel(true);
        submitSearch.setOutputMarkupId(true);
        submitSearch.add(AttributeModifier.append("class", "btn btn-primary"));
        return submitSearch;
    }

    /**
     * Called when user types or changes the text field.
     */
    public void updateSearchModel(@NotNull AjaxRequestTarget target, String newValue) {
        getModel().setObject(Objects.requireNonNullElse(newValue, ""));
        target.add(get(ID_CONTAINER));
    }


    protected abstract void searchPerformed(AjaxRequestTarget ajaxRequestTarget);
}
