/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

@Component
public class ResourceObjectClassFactory extends AbstractObjectClassFactory {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }


    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        PrismObjectWrapper<?> objectWrapper = wrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return false;
        }

        ObjectType object = objectWrapper.getObject().asObjectable();
        if (!(object instanceof ResourceType)) {
            return false;
        }
        return wrapper.getPath().lastName().equivalent(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS)
                || wrapper.getPath().lastName().equivalent(ResourceObjectTypeDefinitionType.F_AUXILIARY_OBJECT_CLASS);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        InputPanel panel = super.getPanel(panelCtx);

        if (panelCtx.getRealValueModel().getObject() != null) {
            return panel;
        }

        if (!panelCtx.unwrapWrapperModel().getPath().lastName().equivalent(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS)) {
            return panel;
        }

        if (panel instanceof AutoCompleteQNamePanel) {
            Collection<QName> choices = ((AutoCompleteQNamePanel<QName>) panel).loadChoices();
            if (choices != null && choices.size() == 1) {
                panelCtx.getRealValueModel().setObject(choices.iterator().next());
            }
        }
        return panel;
    }
}
