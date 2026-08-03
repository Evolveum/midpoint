/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
public class ComplexTypeExtensionTypePanelFactory extends DropDownChoicePanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(ComplexTypeDefinitionType.class) != null
                && ComplexTypeDefinitionType.F_EXTENSION.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    protected List<QName> getTypesList(PrismPropertyPanelContext<QName> panelCtx) {
        List<QName> types = new ArrayList<>(ObjectTypeListUtil.createObjectTypeList());
        types.add(AssignmentType.COMPLEX_TYPE);
        return ObjectTypeListUtil.sortTypesList(types);
    }

    @Override
    public Integer getOrder() {
        return 101;
    }
}
