/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.search.OidSearchItemDefinition;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.PropertyModel;

public class DebugSearchFragment<O extends ObjectType> extends Fragment {

    private static final String ID_SEARCH = "search";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_SHOW_ALL_ITEMS_CHECK = "showAllItemsCheck";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_OID_ITEM = "oidItem";

    public DebugSearchFragment(String id, String markupId, MarkupContainer markupProvider,
            IModel<Search<O>> searchModel, IModel<Boolean> showAllItemsModel) {
        super(id, markupId, markupProvider, searchModel);

        OidSearchItemDefinition oidItemDef = new OidSearchItemDefinition(new PropertyModel<String>(getModel(), Search.F_OID));
        searchModel.getObject().addSpecialItem(oidItemDef.createSearchItem());

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

//        SearchSpecialItemPanel oidItem = new SearchSpecialItemPanel<String>(ID_OID_ITEM, new PropertyModel<String>(getModel(), Search.F_OID)) {
//            @Override
//            protected WebMarkupContainer initSearchItemField(String id) {
//                TextPanel<String> inputPanel = new TextPanel<String>(id, getModelValue());
//                inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px; max-width: 400px !important;"));
//                return inputPanel;
//            }
//
//            @Override
//            protected IModel<String> createLabelModel() {
//                return getPageBase().createStringResource("SearchPanel.oid");
//            }
//
//            @Override
//            protected IModel<String> createHelpModel() {
//                return getPageBase().createStringResource("SearchPanel.oid.help");
//            }
//        };
//        add(oidItem);
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
