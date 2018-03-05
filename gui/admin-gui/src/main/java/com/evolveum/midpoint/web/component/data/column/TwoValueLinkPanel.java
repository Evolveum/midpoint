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

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author katkav
 */
public class TwoValueLinkPanel<T extends Serializable> extends Panel {

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";

    public TwoValueLinkPanel(String id, IModel<String> label, IModel<String> description) {
        super(id);

        AjaxLink link = new AjaxLink(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                TwoValueLinkPanel.this.onClick(target);
            }

//            @Override
//            public String getBeforeDisabledLink() {
//                return null;
//            }
//
//            @Override
//            public String getAfterDisabledLink() {
//                return null;
//            }
        };
        link.add(new Label(ID_LABEL, label));
        link.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return TwoValueLinkPanel.this.isEnabled();
            }
        });
        add(link);
        add(new Label(ID_DESCRIPTION, description));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }
}
