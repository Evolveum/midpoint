/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFormPanel extends BasePanel<Search> {

    private static final String ID_SEARCH = "search";
    private static final String ID_SEARCH_FORM = "searchForm";

    public SearchFormPanel(String id, IModel<Search> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, getModel()) {

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                SearchFormPanel.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);
    }

    protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

    }
}
