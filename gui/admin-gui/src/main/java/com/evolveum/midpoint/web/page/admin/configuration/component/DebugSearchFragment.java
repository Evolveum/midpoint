/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.StringResourceModel;

public class DebugSearchFragment extends Fragment {

    private static final String ID_SEARCH = "search";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_SHOW_ALL_ITEMS_CHECK = "showAllItemsCheck";

    public DebugSearchFragment(String id, String markupId, MarkupContainer markupProvider,
                               IModel<Search> model, IModel<Boolean> showAllItemsModel) {
        super(id, markupId, markupProvider, model);

        initLayout(showAllItemsModel);
    }

    private <O extends ObjectType> IModel<Search<O>> getModel() {
        //noinspection unchecked
        return (IModel<Search<O>>) getDefaultModel();
    }

    private void initLayout(IModel<Boolean> showAllItemsModel) {
        CheckBoxPanel zipCheck = new CheckBoxPanel(ID_ZIP_CHECK, new Model<>(false),
                new StringResourceModel("pageDebugList.zipCheck"), null);
        add(zipCheck);

        CheckBoxPanel showAllItemsCheck = new CheckBoxPanel(ID_SHOW_ALL_ITEMS_CHECK, showAllItemsModel,
                new StringResourceModel("pageDebugList.showAllItems"), null);
        add(showAllItemsCheck);

        SearchPanel searchPanel = new SearchPanel<>(ID_SEARCH, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                DebugSearchFragment.this.searchPerformed(target);
            }
        };
        searchPanel.setOutputMarkupId(true);
        add(searchPanel);
    }

    public CheckBoxPanel getZipCheck() {
        return (CheckBoxPanel) get(ID_ZIP_CHECK);
    }

    protected void searchPerformed(AjaxRequestTarget target) {
    }
}
