/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class ProtectedStringPropertyPanel extends PrismPropertyPanel<ProtectedStringType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";

    public ProtectedStringPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item) {
        ItemName itemName = item.getModelObject() != null ? item.getModelObject().getParent().getItemName() : null;
        Component panel;
        if (PasswordType.F_HINT.equivalent(itemName)) {
            panel = new PasswordHintPanel(ID_PANEL, new ItemRealValueModel<>(item.getModel()), getPasswordModel(item.getModelObject()),
                    getModelObject() != null && getModelObject().isReadOnly());
        } else {
            panel = new PasswordPanel(ID_PANEL, new ItemRealValueModel<>(item.getModel()),
                    getModelObject() != null && getModelObject().isReadOnly(),
                    item.getModelObject() == null || item.getModelObject().getRealValue() == null,
                    getPrismObjectParentIfExist()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void changePasswordPerformed() {
                    PrismPropertyValueWrapper<ProtectedStringType> itemModel = item.getModelObject();
                    if (itemModel != null) {
                        itemModel.setStatus(ValueStatus.MODIFIED);
                    }
                }

            };
        }
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;
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

    private LoadableModel<ProtectedStringType> getPasswordModel(PrismPropertyValueWrapper<ProtectedStringType> hintValueWrapper) {
        return new LoadableModel<ProtectedStringType>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected ProtectedStringType load() {
                if (hintValueWrapper == null) {
                    return null;
                }
                PrismContainerValueWrapper<PasswordType> passwordContainer = hintValueWrapper.getParentContainerValue(PasswordType.class);
                try {
                    PrismPropertyWrapper<ProtectedStringType> passwordWrapper = passwordContainer.findProperty(PasswordType.F_VALUE);
                    return passwordWrapper != null ? passwordWrapper.getValue().getRealValue() : null;
                } catch (SchemaException e) {
                    //nothing to do here
                }
                return null;
            }
        };
    }
}
