/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class LabeledLinkPanel extends Panel {

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    public LabeledLinkPanel(String id, IModel<String> label) {
        super(id);

        initLayout(label);
    }

    private void initLayout(IModel<String> label) {
        AjaxLink link = new AjaxLink(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                LabeledLinkPanel.this.onClick(target);
            }
        };
        add(link);

        link.add(new Label(ID_LABEL, label));
    }

    public void onClick(AjaxRequestTarget target) {

    }
}
