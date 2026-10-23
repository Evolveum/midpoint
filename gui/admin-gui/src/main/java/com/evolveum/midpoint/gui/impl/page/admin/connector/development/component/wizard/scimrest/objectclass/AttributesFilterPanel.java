/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

/**
 * Search field and checkbox filter for an attribute table.
 */
public class AttributesFilterPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_FILTER = "filter";
    private static final String ID_FILTER_LABEL = "filterLabel";

    private final IModel<Boolean> filterModel;
    private final IModel<String> filterLabelModel;
    private final SerializableConsumer<AjaxRequestTarget> onChange;

    public AttributesFilterPanel(String id, IModel<String> searchTextModel, IModel<Boolean> filterModel,
            IModel<String> filterLabelModel, SerializableConsumer<AjaxRequestTarget> onChange) {
        super(id, searchTextModel);
        this.filterModel = filterModel;
        this.filterLabelModel = filterLabelModel;
        this.onChange = onChange;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createSearchForm());
        add(createFilterCheckBox());
        add(new Label(ID_FILTER_LABEL, filterLabelModel));
    }

    private MidpointForm<?> createSearchForm() {
        MidpointForm<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        searchForm.add(new TextField<>(ID_SEARCH_TEXT, getModel()));

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onChange.accept(target);
            }
        };
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);
        return searchForm;
    }

    private CheckBox createFilterCheckBox() {
        CheckBox filter = new CheckBox(ID_FILTER, filterModel);
        filter.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onChange.accept(target);
            }
        });
        return filter;
    }
}
