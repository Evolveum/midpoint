/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author skublik
 */

public class LabelWithHelpPanel extends BasePanel<String>{

    private static final String ID_NAME = "name";
    private static final String ID_HELP = "help";

    public LabelWithHelpPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label name = new Label(ID_NAME, getModel());
        name.setOutputMarkupId(true);
        add(name);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = getHelpModel();
        help.add(AttributeModifier.replace("data-original-title",
                createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.add(AttributeModifier.replace("aria-label",
                createStringResource("LabelWithHelpPanel.tooltipFor", getModelObject())));
        help.setOutputMarkupId(true);
        add(help);
    }

    protected IModel<String> getHelpModel() {
        return Model.of("");
    }

}
