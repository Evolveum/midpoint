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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class BrowserPopupPanel<T extends Serializable> extends SearchPopupPanel<T> {

    private static final String ID_BROWSER_INPUT = "browserInput";
    private static final String ID_BROWSE = "browse";

    public BrowserPopupPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        IModel value = new PropertyModel(getModel(), SearchValue.F_LABEL);
        TextField input = new TextField(ID_BROWSER_INPUT, value);
        add(input);

        AjaxLink browse = new AjaxLink(ID_BROWSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browsePerformed(target);
            }
        };
        add(browse);
    }

    protected void browsePerformed(AjaxRequestTarget target) {

    }
}
