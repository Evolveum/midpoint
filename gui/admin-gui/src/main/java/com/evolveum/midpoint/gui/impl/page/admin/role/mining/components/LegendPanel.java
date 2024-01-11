/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

public class LegendPanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    LoadableModel<String> loadableModel;

    public LegendPanel(String id, LoadableModel<String> loadableModel) {
        super(id);
        this.loadableModel = loadableModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        Label label = new Label(ID_LABEL, loadableModel){
            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.add(AttributeModifier.replace("style", "color: transparent; "
                        + "background-color: " + loadableModel.getObject() + ";"));
            }
        };
        label.setOutputMarkupId(true);

        container.add(label);
        add(container);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }
}
