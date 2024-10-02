/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeParticipantType;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author skublik
 */
@Component
public class QNameTextPanelFactory extends DropDownChoicePanelFactory implements Serializable{

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(SimulatedReferenceTypeParticipantType.class) != null
                && (SimulatedReferenceTypeParticipantType.F_PRIMARY_BINDING_ATTRIBUTE_REF.equivalent(wrapper.getItemName())
                || SimulatedReferenceTypeParticipantType.F_SECONDARY_BINDING_ATTRIBUTE_REF.equivalent(wrapper.getItemName())
                || SimulatedReferenceTypeParticipantType.F_LOCAL_ITEM_NAME.equivalent(wrapper.getItemName()))) {
            return true;
        }

        if (wrapper.getParentContainerValue(SimulatedReferenceTypeDefinitionType.class) != null
                && SimulatedReferenceTypeDefinitionType.F_NAME.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        return new TextPanel<>(panelCtx.getComponentId(),
                panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false);
    }

    @Override
    public Integer getOrder() {
        return 900;
    }
}
