/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.util.ExpressionUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Collection;

/**
 * Created by honchar
 */
public class ExpressionWrapper extends PrismPropertyWrapperImpl<ExpressionType> {

    private static final QName CUSTOM_Q_NAME = new QName("com.evolveum.midpoint.gui", "customExtenstionType");

    private ConstructionType construction;

    public ExpressionWrapper(@Nullable PrismContainerValueWrapper parent, PrismProperty<ExpressionType> property, ItemStatus status) {
        super(parent, property, status);
    }

    public boolean isConstructionExpression() {
        PrismContainerWrapperImpl outboundContainer = getParent() != null ? (PrismContainerWrapperImpl) getParent().getParent() : null;
        if (outboundContainer != null && MappingType.class.equals(outboundContainer.getCompileTimeClass())) {
            PrismContainerValueWrapperImpl outboundValue = (PrismContainerValueWrapperImpl) outboundContainer.getParent();
            if (outboundValue != null) {
                PrismContainerWrapperImpl associationContainer = (PrismContainerWrapperImpl) outboundValue.getParent();
                if (associationContainer != null &&
                        (ResourceObjectAssociationType.class.equals(associationContainer.getCompileTimeClass()) ||
                                ResourceAttributeDefinitionType.class.equals(associationContainer.getCompileTimeClass()))) {
                    PrismContainerValueWrapperImpl constructionContainer = (PrismContainerValueWrapperImpl) associationContainer.getParent();
                    if (constructionContainer != null && constructionContainer.getRealValue() instanceof ConstructionType) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isAssociationExpression() {
        if (!getPath().last().equals(MappingType.F_EXPRESSION.last())) {
            return false;
        }
        PrismContainerWrapperImpl outboundContainer = getParent() != null ? (PrismContainerWrapperImpl) getParent().getParent() : null;
        if (outboundContainer != null && MappingType.class.equals(outboundContainer.getCompileTimeClass())) {
            PrismContainerValueWrapperImpl outboundValue = (PrismContainerValueWrapperImpl) outboundContainer.getParent();
            if (outboundValue != null) {
                PrismContainerWrapperImpl associationContainer = (PrismContainerWrapperImpl) outboundValue.getParent();
                if (associationContainer != null &&
                        ResourceObjectAssociationType.class.equals(associationContainer.getCompileTimeClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAttributeExpression() {
        if (!getPath().last().equals(MappingType.F_EXPRESSION.last())) {
            return false;
        }
        PrismContainerWrapperImpl outboundContainer = getParent() != null ? (PrismContainerWrapperImpl) getParent().getParent() : null;
        if (outboundContainer != null && MappingType.class.isAssignableFrom(outboundContainer.getCompileTimeClass())) {
            PrismContainerValueWrapperImpl outboundValue = (PrismContainerValueWrapperImpl) outboundContainer.getParent();
            if (outboundValue != null) {
                PrismContainerWrapperImpl attributeContainer = (PrismContainerWrapperImpl) outboundValue.getParent();
                if (attributeContainer != null &&
                        (ResourceAttributeDefinitionType.class.equals(attributeContainer.getCompileTimeClass())
                                || ResourceBidirectionalMappingType.class.equals(attributeContainer.getCompileTimeClass())
                        || ResourcePasswordDefinitionType.class.equals(attributeContainer.getCompileTimeClass()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isFocusMappingExpression() {
        if (!getPath().last().equals(MappingType.F_EXPRESSION.last())) {
            return false;
        }
        PrismContainerWrapperImpl mappingContainer = getParent() != null ? (PrismContainerWrapperImpl) getParent().getParent() : null;
        if (mappingContainer != null && MappingType.class.isAssignableFrom(mappingContainer.getCompileTimeClass())) {
            PrismContainerValueWrapperImpl mappingValue = (PrismContainerValueWrapperImpl) mappingContainer.getParent();
            if (mappingValue != null) {
                PrismContainerWrapperImpl mappingsContainer = (PrismContainerWrapperImpl) mappingValue.getParent();
                if (mappingsContainer != null &&
                        MappingsType.class.equals(mappingsContainer.getCompileTimeClass())
                        && AssignmentType.F_FOCUS_MAPPINGS.equivalent(mappingsContainer.getItemName())) {
                    return  true;
                }
            }
        }
        return false;
    }

    public ConstructionType getConstruction() {
        if (getParent() == null) {
            return construction;
        }

        if (construction == null) {
            PrismContainerWrapperImpl outboundContainer = getParent().getParent();
            if (outboundContainer == null) {
                return construction;
            }

            PrismContainerValueWrapperImpl outboundValue = (PrismContainerValueWrapperImpl) outboundContainer.getParent();
            if (outboundValue == null) {
                return construction;
            }

            PrismContainerWrapperImpl associationContainer = (PrismContainerWrapperImpl) outboundValue.getParent();
            if (associationContainer != null) {
                PrismContainerValueWrapperImpl constructionContainer = (PrismContainerValueWrapperImpl) associationContainer.getParent();
                if (constructionContainer != null && constructionContainer.getRealValue() instanceof ConstructionType) {
                    construction = (ConstructionType) constructionContainer.getRealValue();
                }
            }

        }

        return construction;
    }

    public void setConstruction(ConstructionType construction) {
        this.construction = construction;
    }

    @Override
    public Integer getDisplayOrder() {
        if (isAssociationExpression() || isAttributeExpression()) {
            //todo MAX_VALUE doesn't guarantee that expression property
            //will be displayed the last, as further there will be properties
            //without any displayOrder displayed
            return Integer.MAX_VALUE;
        } else {
            return super.getDisplayOrder();
        }
    }

    @Override
    public @NotNull QName getTypeName() {
        return CUSTOM_Q_NAME;
    }

    @Override
    protected <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> void addValueToDelta(
            PrismPropertyValueWrapper<ExpressionType> value, D delta) throws SchemaException {
        if (!ExpressionUtil.isEmpty(value.getRealValue())) {
            super.addValueToDelta(value, delta);
            return;
        }
        if (value.getOldValue() != null && !ExpressionUtil.isEmpty(value.getOldValue().getRealValue())) {
            value.setStatus(ValueStatus.DELETED);
            super.addValueToDelta(value, delta);
        }
    }
}
