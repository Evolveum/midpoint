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

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ButtonPanel<T extends Serializable> extends Panel {

    public ButtonPanel(String id, IModel<String> label, ButtonType type) {
        super(id);
        AjaxLinkButton link = new AjaxLinkButton("link", type, label) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ButtonPanel.this.onClick(target);
            }
        };
        add(link);
    }

    public void onClick(AjaxRequestTarget target) {
    }
}
