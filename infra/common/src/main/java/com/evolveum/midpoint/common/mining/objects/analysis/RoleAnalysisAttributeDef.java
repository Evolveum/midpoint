/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class RoleAnalysisAttributeDef implements Serializable {

    private ItemPath path;
    private String displayValue;

    private final Class<? extends FocusType> parentType;
    private final ItemDefinition<?> definition;

    public RoleAnalysisAttributeDef(ItemPath path,
            ItemDefinition<?> definition,
            Class<? extends FocusType> parentType) {
        this.path = path;
        this.definition = definition;
        this.parentType = parentType;
    }

    public ItemPath getPath() {
        return path;
    }

    private boolean isContainer() {
        return definition instanceof PrismContainerDefinition;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        if (!isContainer()) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
            if (property != null) {
                Object object = property.getRealValue();
                return extractRealValue(object);
            }
        }
        return null;
    }

    //TODO
    public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            boolean isMultiValue = !item.isSingleValue();

            if (isMultiValue) {
                Collection<?> realValues = item.getRealValues();
                for (Object realValue : realValues) {
                    if (realValue instanceof ObjectReferenceType refValue) {
                        resolvedValues.add(refValue.getOid());
                    } else if (realValue != null) {
                        resolvedValues.add(realValue.toString());
                    }
                }
            } else {
                Object realValue = item.getRealValue();
                if (realValue != null) {
                    resolvedValues.add(realValue.toString());
                }
            }
        }
        return resolvedValues;
    }

    public static String extractRealValue(Object object) {
        if (object != null) {
            if (object instanceof PolyString) {
                return ((PolyString) object).getOrig();
            } else if (object instanceof ObjectReferenceType) {
                return ((ObjectReferenceType) object).getOid();
            } else if (object instanceof PrismPropertyValueImpl) {
                Object realValue = ((PrismPropertyValueImpl<?>) object).getRealValue();
                if (realValue != null) {
                    return realValue.toString();
                }
            } else {
                return object.toString();
            }
        }
        return null;
    }

    public ObjectQuery getQuery(String value) {
        if (definition instanceof PrismReferenceDefinition) {
            return PrismContext.get().queryFor(parentType)
                    .item(getPath()).ref(value)
                    .build();
        }
        return PrismContext.get().queryFor(parentType)
                .item(getPath()).eq(value)
                .build();
    }

    public static @NotNull RoleAnalysisAttributeDef getRoleAnalysisArchetypeDef(Class<? extends FocusType> parentType) {
        PrismObjectDefinition<?> userDefinition = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(parentType);

        ItemPath itemPath = ItemPath.create(AssignmentHolderType.F_ARCHETYPE_REF);
        ItemDefinition<?> def = userDefinition.findItemDefinition(itemPath);
        return new RoleAnalysisAttributeDef(itemPath, def, parentType);
    }

    public boolean isMultiValue() {
        return definition.isMultiValue();
    }

    public boolean isReference() {
        return definition instanceof PrismReferenceDefinition;
    }
}
