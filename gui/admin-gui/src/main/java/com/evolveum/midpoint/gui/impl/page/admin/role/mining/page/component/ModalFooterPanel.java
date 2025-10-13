/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class ModalFooterPanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_COMPONENTS = "components";

    public ModalFooterPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RepeatingView repeatingView = new RepeatingView(ID_COMPONENTS);
        container.add(repeatingView);

        addComponentButton(repeatingView);

    }

    protected void addComponentButton(RepeatingView repeatingView) {

    }


}
