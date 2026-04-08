/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.io.Serial;
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

        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                WebComponentUtil.updateAjaxLinkAttributesForCtrlClickRedirection(attributes);
            }

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
        link.add(AttributeAppender.replace("href", urlForLinkRedirection()));
        link.add(new Label(ID_LABEL, label));
        link.add(new EnableBehaviour(this::isEnabled));
        add(link);
        add(new Label(ID_DESCRIPTION, description));
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }

    protected String urlForLinkRedirection() {
        return null;
    }
}
