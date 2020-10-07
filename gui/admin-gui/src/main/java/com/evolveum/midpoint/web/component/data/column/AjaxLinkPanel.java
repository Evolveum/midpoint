/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class AjaxLinkPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    public AjaxLinkPanel(String id, IModel labelModel) {
        super(id);

        AjaxLink<String> link = new AjaxLink<String>(ID_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkPanel.this.onClick(target);
            }

        };
        Label label;
        if(labelModel.getObject() instanceof QName) {
            label = new Label(ID_LABEL, new IModel<String>() {

                @Override
                public String getObject() {
                    return ((QName) labelModel.getObject()).getLocalPart();
                }
            });
        } else {
            label = new Label(ID_LABEL, labelModel);
        }
        link.add(label);
        link.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return AjaxLinkPanel.this.isEnabled();
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
