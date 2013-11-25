/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class TabbedPanel<T extends ITab> extends AjaxTabbedPanel<T> {

    public TabbedPanel(String id, List<T> tabs) {
        super(id, tabs);
    }

    public TabbedPanel(String id, List<T> tabs, IModel<Integer> model) {
        super(id, tabs, model);
    }

    @Override
    protected WebMarkupContainer newTabsContainer(String id) {
        WebMarkupContainer tabs = new WebMarkupContainer(id);
        tabs.setOutputMarkupId(true);
        return tabs;
    }

    @Override
    protected Component newTitle(final String titleId, final IModel titleModel, final int index) {
        Label label = new Label(titleId, titleModel);
        label.setRenderBodyOnly(true);
        return label;
    }

    @Override
    protected String getSelectedTabCssClass() {
        return "active";
    }

    @Override
    protected String getLastTabCssClass() {
        return "";
    }
}
