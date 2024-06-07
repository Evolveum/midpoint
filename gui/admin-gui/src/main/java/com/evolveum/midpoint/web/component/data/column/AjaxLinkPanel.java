/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

/**
 * @author lazyman
 */
public class AjaxLinkPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    public AjaxLinkPanel(String id, IModel labelModel) {
        super(id);

        AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkPanel.this.onClick(target);
            }

        };
        Label label = new Label(ID_LABEL, labelModel);

        link.add(label);
        link.add(new EnableBehaviour(() -> AjaxLinkPanel.this.isEnabled()));
        add(link);
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }

    public AjaxLink<String> getLink() {
        return (AjaxLink<String>) get(ID_LINK);
    }
}
