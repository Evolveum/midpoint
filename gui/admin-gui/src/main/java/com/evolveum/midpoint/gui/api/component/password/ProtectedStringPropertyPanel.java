/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.io.Serial;

/**
 * Created by honchar
 */
public class ProtectedStringPropertyPanel extends PrismPropertyPanel<ProtectedStringType> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";

    public ProtectedStringPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item) {
        Component panel = new PasswordPropertyPanel(ID_PANEL, new ItemRealValueModel<>(item.getModel()),
                    getModelObject() != null && getModelObject().isReadOnly(),
                    item.getModelObject() == null || item.getModelObject().getRealValue() == null,
                    getPrismObjectParentIfExist()) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected void changePasswordPerformed() {
                    PrismPropertyValueWrapper<ProtectedStringType> itemModel = item.getModelObject();
                    if (itemModel != null) {
                        itemModel.setStatus(ValueStatus.MODIFIED);
                    }
                }

                @Override
                protected boolean canEditPassword() {
                    return isEditable();
                }

                @Override
                protected boolean isPasswordLimitationPopupVisible() {
                    return useGlobalValuePolicy();
                }
            };
//        }
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;
    }

    private boolean useGlobalValuePolicy() {
        return getModelObject() == null || getModelObject().getPath() == null
                || !getModelObject().getPath().startsWith(ObjectType.F_EXTENSION);
    }

    private boolean isEditable() {
        PrismPropertyWrapper<ProtectedStringType> propertyWrapper = getModelObject();
        return propertyWrapper != null && !propertyWrapper.isReadOnly();
    }

    private <O extends ObjectType> PrismObject<O>getPrismObjectParentIfExist() {
        PrismObject<O> prismObject = null;
        Object wrapper = getModelObject();
        while (wrapper != null) {
            if (wrapper instanceof ItemWrapper){
                wrapper = ((ItemWrapper)wrapper).getParent();
            } else if (wrapper instanceof PrismValueWrapper) {
                wrapper = ((PrismValueWrapper)wrapper).getParent();
            } else {
                return null;
            }
            if (wrapper instanceof PrismObjectWrapper) {
                prismObject = ((PrismObjectWrapper)wrapper).getObject();
                break;
            }
        }
        return prismObject;
    }

}
