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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Search field and checkbox filters for an attribute table.
 *
 * @param <T> row type of the table
 */
public class AttributesFilterPanel<T> extends BasePanel<AttributesFilterState<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_FILTERS = "filters";
    private static final String ID_FILTER = "filter";
    private static final String ID_FILTER_LABEL = "filterLabel";

    private final SerializableConsumer<AjaxRequestTarget> onChange;

    /**
     * Creates the search field with the checkbox filters.
     *
     * @param id component id
     * @param model search text and checkbox filters
     * @param onChange action called after the search text or a filter changes
     */
    public AttributesFilterPanel(String id, IModel<AttributesFilterState<T>> model, SerializableConsumer<AjaxRequestTarget> onChange) {
        super(id, model);
        this.onChange = onChange;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createSearchForm());
        add(createFilters());
    }

    private MidpointForm<?> createSearchForm() {
        MidpointForm<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        searchForm.add(new TextField<>(ID_SEARCH_TEXT,
                LambdaModel.of(getModel(), AttributesFilterState::getSearchText, AttributesFilterState::setSearchText)));

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

    private ListView<AttributesFilter<T>> createFilters() {
        return new ListView<>(ID_FILTERS, () -> getModelObject().getFilters()) {

            @Override
            protected void populateItem(ListItem<AttributesFilter<T>> item) {
                item.add(createFilterCheckBox(item.getModelObject().on()));
                item.add(new Label(ID_FILTER_LABEL, item.getModelObject().label()));
                item.add(new VisibleBehaviour(() -> item.getModelObject().visible().getObject()));
            }
        };
    }

    private CheckBox createFilterCheckBox(IModel<Boolean> filterModel) {
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
