/*
 * Copyright (c) 2010-2017 Evolveum
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

/**
 * @author lazyman
 */
public class LinkPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    public LinkPanel(String id, IModel<String> labelModel) {
        super(id);

        AjaxLink<String> link = new AjaxLink<String>(ID_LINK) {
			private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
                LinkPanel.this.onClick(target);
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
        link.add(new Label(ID_LABEL, labelModel));
        link.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return LinkPanel.this.isEnabled();
            }
        });
        add(link);
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }
}
