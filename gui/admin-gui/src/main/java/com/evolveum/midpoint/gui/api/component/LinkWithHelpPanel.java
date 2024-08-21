/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;



public class LinkWithHelpPanel extends BasePanel<String> {

    private static final String ID_LINK = "link";
    private static final String ID_HELP = "help";

    public LinkWithHelpPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_LINK, getModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClick(target);
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        add(ajaxLinkPanel);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = getHelpModel();
        help.add(AttributeModifier.replace("data-original-title",
                createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.setOutputMarkupId(true);
        add(help);
    }

    protected void onClick(AjaxRequestTarget target) {
    }

    protected IModel<String> getHelpModel() {
        return Model.of("");
    }

}
