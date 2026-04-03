/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import java.io.Serial;

/**
 * This panel should be used in case the link should handle
 * CTRL+click event to open the target page in the new browser tab.
 */
public abstract class AjaxLinkWithNewTabSupport extends Panel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";

    private final IModel<String> labelModel;

    public AjaxLinkWithNewTabSupport(String id) {
        this(id, null);
    }

    public AjaxLinkWithNewTabSupport(String id, IModel<String> labelModel) {
        super(id);
        this.labelModel = labelModel;
    }

    protected void onInitialize() {
        super.onInitialize();

        AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                WebComponentUtil.updateAjaxLinkAttributesForCtrlClickRedirection(attributes);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkWithNewTabSupport.this.onClick(target);
            }
        };
        configureNavigationLink(link);
        link.add(AttributeAppender.append("class", getLinkAdditionalStyle()));
        link.add(AttributeAppender.append("title", getLinkDescriptiveTitle()));
        link.setOutputMarkupId(true);

        Label label = new Label(ID_LABEL, labelModel);
        link.add(label);
        link.add(new EnableBehaviour(AjaxLinkWithNewTabSupport.this::isEnabled));
        add(link);
    }

    private void configureNavigationLink(AjaxLink<String> link) {
        String navigationUrl = getNavigationUrl();
        if (navigationUrl != null) {
            link.add(AttributeModifier.replace("href", navigationUrl));
        }
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }

    protected abstract String getNavigationUrl();

    protected String getLinkAdditionalStyle() {
        return null;
    }

    protected String getLinkDescriptiveTitle() {
        return null;
    }
}
