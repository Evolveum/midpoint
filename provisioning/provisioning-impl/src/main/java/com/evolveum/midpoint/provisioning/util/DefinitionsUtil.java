/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Temporary place for this code.
 *
 * FIXME
 */
public class DefinitionsUtil {

    public static void applyDefinition(ProvisioningContext ctx, ObjectQuery query)
            throws SchemaException {
        if (query == null) {
            return;
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            return;
        }
        applyDefinition(filter, ctx.getObjectDefinitionRequired());
    }

    public static void applyDefinition(@NotNull ObjectFilter filter, @NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        com.evolveum.midpoint.prism.query.Visitor visitor = subFilter -> {
            if (subFilter instanceof PropertyValueFilter) {
                PropertyValueFilter<?> valueFilter = (PropertyValueFilter<?>) subFilter;
                ItemDefinition<?> definition = valueFilter.getDefinition();
                if (definition instanceof ShadowSimpleAttributeDefinition) {
                    return; // already has a resource-related definition
                }
                if (!ShadowType.F_ATTRIBUTES.equivalent(valueFilter.getParentPath())) {
                    return;
                }
                QName attributeName = valueFilter.getElementName();
                ShadowSimpleAttributeDefinition<?> attributeDefinition =
                        objectDefinition.findSimpleAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new TunnelException(
                            new SchemaException("No definition for attribute " + attributeName + " in " + filter));
                }
                //noinspection unchecked,rawtypes
                valueFilter.setDefinition((ShadowSimpleAttributeDefinition) attributeDefinition);
            }
        };
        try {
            filter.accept(visitor);
        } catch (TunnelException te) {
            throw (SchemaException) te.getCause();
        }
    }
}
