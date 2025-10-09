/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

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

    private static final Trace LOGGER = TraceManager.getTrace(LinkPanel.class);

    public LinkPanel(String id, IModel labelModel) {
        super(id);

        Link<String> link = new Link<>(ID_LINK) {

            @Override
            public void onClick() {
                LinkPanel.this.onClick();
            }
        };

        Label label = new Label(ID_LABEL, () -> {
            Object obj = labelModel.getObject();
            if (obj instanceof QName) {
                return ((QName) obj).getLocalPart();
            }

            return obj;
        });
        link.add(label);
        link.add(new EnableBehaviour(() -> LinkPanel.this.isEnabled()));
        add(link);
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick() {
    }
}
