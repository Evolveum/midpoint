/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
