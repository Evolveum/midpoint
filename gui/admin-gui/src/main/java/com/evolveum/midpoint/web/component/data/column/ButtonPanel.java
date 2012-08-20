/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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

    public ButtonPanel(String id, IModel<String> label) {
        this(id, label, ButtonType.SIMPLE);
    }

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
