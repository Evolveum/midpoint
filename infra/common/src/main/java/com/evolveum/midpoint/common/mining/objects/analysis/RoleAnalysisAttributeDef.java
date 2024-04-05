/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class RoleAnalysisAttributeDef implements Serializable {

    ItemPath path;
    boolean isContainer;
    String displayValue;
    ObjectQuery query;
    Class<? extends ObjectType> classType;
    IdentifierType identifierType;

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            Class<? extends ObjectType> classType) {
        this.path = path;
        this.isContainer = isContainer;
        this.classType = classType;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            Class<? extends ObjectType> classType,
            IdentifierType identifierType) {
        this.path = path;
        this.isContainer = isContainer;
        this.classType = classType;
        this.identifierType = identifierType;
    }

    public RoleAnalysisAttributeDef(ItemPath path,
            boolean isContainer,
            String displayValue,
            Class<? extends ObjectType> classType,
            IdentifierType identifierType) {
        this.path = path;
        this.isContainer = isContainer;
        this.displayValue = displayValue;
        this.classType = classType;
        this.identifierType = identifierType;
    }

    public void calculateZScore(int objectWithExactAttributeCount, RoleAnalysisAttributeDef attributeDef) {
        // TODO
    }

    public ItemPath getPath() {
        return path;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String resolveSingleValueItem(PrismObject<?> prismObject, ItemPath itemPath) {
        if (!isContainer) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
            if (property != null) {
                Object object = property.getRealValue();
                return extractRealValue(object);
            }
        }
        return null;
    }

    public Set<String> resolveMultiValueItem(PrismObject<?> prismObject, ItemPath itemPath) {
        return null;
    }

    protected static String extractRealValue(Object object) {
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
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public Class<? extends ObjectType> getClassType() {
        return classType;
    }

    public void setClassType(Class<? extends ObjectType> classType) {
        this.classType = classType;
    }

    public enum IdentifierType {
        OID,
        FINAL
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

}
