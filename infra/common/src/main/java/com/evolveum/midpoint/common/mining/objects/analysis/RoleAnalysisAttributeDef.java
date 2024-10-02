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
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class RoleAnalysisAttributeDef implements Serializable {

    ItemPath path;
//    boolean isContainer;
    String displayValue;
    ObjectQuery query;
    Class<? extends ObjectType> targetClassType;
    Class<? extends ObjectType> associatedClassType;

    private ItemDefinition<?> definition;

    IdentifierType identifierType;

    public RoleAnalysisAttributeDef(ItemPath path,
            ItemDefinition<?> definition) {
        this.path = path;
        this.definition = definition;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            Class<? extends ObjectType> classType) {
        this.path = path;
//        this.isContainer = isContainer;
        this.targetClassType = classType;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            String displayValue,
            Class<? extends ObjectType> classType,
            IdentifierType identifierType) {
        this.path = path;
//        this.isContainer = isContainer;
        this.displayValue = displayValue;
        this.targetClassType = classType;
        this.identifierType = identifierType;
    }

    public ItemPath getPath() {
        return path;
    }

    public boolean isContainer() {
        return definition instanceof PrismContainerDefinition;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

//    public void setContainer(boolean container) {
//        isContainer = container;
//    }

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
//                    return item.getRealValues()
//                            .stream()
//                            .filter(value -> value != null)
//                            .map(value -> ((ObjectReferenceType) value).getOid())
//                            .collect(Collectors.toSet());
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
            return PrismContext.get().queryFor(UserType.class)
                    .item(getPath()).ref(value)
                    .build();
        }
        return PrismContext.get().queryFor(UserType.class)
                .item(getPath()).eq(value)
                .build();
//        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public Class<? extends ObjectType> getTargetClassType() {
        return targetClassType;
    }

    public enum IdentifierType {
        OID,
        FINAL
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public Class<? extends ObjectType> getAssociatedClassType() {
        return associatedClassType;
    }

    public void setAssociatedClassType(Class<? extends ObjectType> associatedClassType) {
        this.associatedClassType = associatedClassType;
    }

    public QName getComplexType() {
        Class<? extends ObjectType> classType = getAssociatedClassType();
        if (classType.equals(UserType.class)) {
            return UserType.COMPLEX_TYPE;
        } else if (classType.equals(RoleType.class)) {
            return RoleType.COMPLEX_TYPE;
        }
        return null;
    }

    public boolean isMultiValue() {
        return definition.isMultiValue();
    }

    public boolean isReference() {
        return definition instanceof PrismReferenceDefinition;
    }
}
