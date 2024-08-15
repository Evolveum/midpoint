/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lskublik
 */
@Component
public class AssociationReferenceMappingItemPathPanelFactory extends AssociationAttributeMappingItemPathPanelFactory implements Serializable {

    @Override
    protected ItemName getItemNameForContainerOfAttributes() {
        return AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF;
    }

    @Override
    protected List<DisplayableValue<ItemPathType>> getAttributes(ResourceSchema schema, PrismValueWrapper<ItemPathType> propertyWrapper) {
        ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(schema, propertyWrapper);
        if (assocDef == null) {
            return Collections.emptyList();
        }

        List<DisplayableValue<ItemPathType>> attributes = new ArrayList<>();
        assocDef.getObjectParticipantNames()
                .forEach(key -> attributes.add(createDisplayValue(key)));

        return attributes;
    }
}
