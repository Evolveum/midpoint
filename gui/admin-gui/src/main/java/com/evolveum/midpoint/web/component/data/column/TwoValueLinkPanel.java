/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

        AjaxLink<Void> link = new AjaxLink<Void>(ID_LINK) {

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

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }
}
