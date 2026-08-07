/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationTypeDefinitionType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;

@Component
public class SchemaEnumBaseTypePanelFactory extends SchemaItemTypePanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(EnumerationTypeDefinitionType.class) != null
                && EnumerationTypeDefinitionType.F_BASE_TYPE.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    protected List<DisplayableValue<QName>> createValues(PrismPropertyPanelContext<QName> panelCtx) {
        List<DisplayableValue<QName>> allTypes = new ArrayList<>();
        XsdTypeMapper.getAllTypes().forEach(type -> allTypes.add(createDisplayValue(createLabelForType(null, type), type)));
        return allTypes;
    }
}
