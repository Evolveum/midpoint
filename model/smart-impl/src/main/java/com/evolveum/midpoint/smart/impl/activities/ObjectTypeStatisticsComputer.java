/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Computes statistics for shadow objects of a resource object type.
 */
public class ObjectTypeStatisticsComputer extends AbstractStatisticsComputer<QName> {

    public ObjectTypeStatisticsComputer(@NotNull ResourceObjectTypeDefinition typeDefinition) {
        getStatisticsObject().setSize(0);

        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : typeDefinition.getAttributeDefinitions()) {
            ItemName attrName = attrDef.getItemName();
            registerItem(attrName, toAttributeRef(attrName), isDnAttribute(attrName));
        }
    }

    public void process(ShadowType shadow) {
        incrementSize();

        for (QName attrName : getItemOrder()) {
            List<?> values = ShadowUtil.getAttributeValues(shadow, attrName);
            aggregateItem(attrName, values);
        }
    }

    @Override
    protected QName fromRef(@NotNull ItemPathType ref) {
        return ref.getItemPath().rest().asSingleNameOrFail();
    }
}
