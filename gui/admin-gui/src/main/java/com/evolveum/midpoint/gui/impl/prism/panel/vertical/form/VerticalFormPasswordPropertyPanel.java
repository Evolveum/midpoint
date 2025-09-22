/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.component.password.ProtectedStringPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by honchar
 */
public class VerticalFormPasswordPropertyPanel extends ProtectedStringPropertyPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";

    public VerticalFormPasswordPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        return new VerticalFormPrismPropertyHeaderPanel<>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPasswordPropertyPanel.this);
            }
        };
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item) {
        PrismPropertyValuePanel<ProtectedStringType> panel = new VerticalFormPrismPropertyValuePanel<>(ID_VALUE, item.getModel(), getSettings()) {

            @Override
            protected boolean isRemoveButtonVisible() {
                return false;
            }
        };
        item.add(panel);
        return panel;
    }

    @Override
    protected String getCssClassForValueContainer() {
        return "";
    }
}
