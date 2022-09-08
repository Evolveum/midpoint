/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DebugSearchFragment extends Fragment {

    private static final String ID_SEARCH = "search";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_SHOW_ALL_ITEMS_CHECK = "showAllItemsCheck";
    private static final String ID_SEARCH_FORM = "searchForm";

    private IModel<Boolean> showAllItemsModel;

    public DebugSearchFragment(String id, String markupId, MarkupContainer markupProvider,
            IModel<Search<? extends ObjectType>> model, IModel<Boolean> showAllItemsModel) {
        super(id, markupId, markupProvider, model);
        this.showAllItemsModel = showAllItemsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout(showAllItemsModel);
    }

    private <O extends ObjectType> IModel<Search<O>> getModel() {
        //noinspection unchecked
        return (IModel<Search<O>>) getDefaultModel();
    }

    private void initLayout(IModel<Boolean> showAllItemsModel) {

        createSearchForm();

        AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(zipCheck);

        AjaxCheckBox showAllItemsCheck = new AjaxCheckBox(ID_SHOW_ALL_ITEMS_CHECK, showAllItemsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(showAllItemsCheck);

    }

    private void createSearchForm() {
        final Form<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);
        searchForm.add(createSearchPanel());
    }

    private <O extends ObjectType> WebMarkupContainer createSearchPanel() {
        SearchPanel<O> searchPanel = new SearchPanel<>(ID_SEARCH, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                DebugSearchFragment.this.searchPerformed(target);
            }
        };
        searchPanel.setOutputMarkupId(true);
        return searchPanel;
    }

    public AjaxCheckBox getZipCheck() {
        return (AjaxCheckBox) get(ID_ZIP_CHECK);
    }

    protected void searchPerformed(AjaxRequestTarget target) {
    }
}
