/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class PasswordPropertyPanel  extends PrismPropertyPanel<ProtectedStringType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PASSWORD_PANEL= "passwordPanel";

    public PasswordPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemHeaderPanel.ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item, GuiComponentFactory factory,
            ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler) {

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new ItemRealValueModel<>(item.getModel()),
                    getModelObject() != null && getModelObject().isReadOnly(),
                    item.getModelObject() == null || item.getModelObject().getRealValue() == null ){
            private static final long serialVersionUID = 1L;

            @Override
            protected void changePasswordPerformed(){
                PrismPropertyValueWrapper<ProtectedStringType> itemModel = item.getModelObject();
                if (itemModel != null){
                    itemModel.setStatus(ValueStatus.MODIFIED);
                }
            }

        };
        passwordPanel.setOutputMarkupId(true);
        item.add(passwordPanel);
        return passwordPanel;



    }

    @Override
    protected void createButtons(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item) {
        //nothing to do
    }

}
