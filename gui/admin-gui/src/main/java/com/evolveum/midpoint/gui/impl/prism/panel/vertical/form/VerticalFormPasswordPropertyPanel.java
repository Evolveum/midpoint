/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.component.password.PasswordPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class VerticalFormPasswordPropertyPanel extends PasswordPropertyPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";

    public VerticalFormPasswordPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected Component createHeaderPanel() {
        return new VerticalFormPrismPropertyHeaderPanel<ProtectedStringType>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPasswordPropertyPanel.this);
            }

            @Override
            protected void createRequired(String id) {
                WebMarkupContainer required = new WebMarkupContainer(id);
                required.add(new VisibleBehaviour(() -> false));
                add(required);
            }
        };
    }

    @Override
    protected String getCssClassForValueContainer() {
        return "";
    }
}
