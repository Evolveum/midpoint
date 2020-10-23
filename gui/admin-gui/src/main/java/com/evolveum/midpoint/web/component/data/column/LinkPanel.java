/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 */
public class LinkPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    private static final transient Trace LOGGER = TraceManager.getTrace(LinkPanel.class);

    public LinkPanel(String id, IModel labelModel) {
        super(id);

        Link<String> link = new Link<String>(ID_LINK) {
            @Override
            public void onClick() {
                LinkPanel.this.onClick();
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
                return LinkPanel.this.isEnabled();
            }
        });
        add(link);
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick() {
    }
}
