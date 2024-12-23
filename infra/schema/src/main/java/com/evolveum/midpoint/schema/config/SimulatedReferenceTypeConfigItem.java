/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeDefinitionType;

public class SimulatedReferenceTypeConfigItem
        extends ConfigurationItem<SimulatedReferenceTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public SimulatedReferenceTypeConfigItem(@NotNull ConfigurationItem<SimulatedReferenceTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getName() throws ConfigurationException {
        return QNameUtil.enforceNamespace(
                nonNull(value().getName(), "reference type name"),
                MidPointConstants.NS_RI);
    }

    public @NotNull String getNameLocalPart() throws ConfigurationException {
        return QNameUtil.getLocalPartCheckingNamespace(
                nonNull(value().getName(), "reference type name"),
                MidPointConstants.NS_RI);
    }

    public @NotNull SimulatedReferenceTypeParticipantConfigItem.Subject getSubject()
            throws ConfigurationException {
        return child(
                value().getSubject(),
                SimulatedReferenceTypeParticipantConfigItem.Subject.class,
                SimulatedReferenceTypeDefinitionType.F_SUBJECT);
    }

    public @NotNull SimulatedReferenceTypeParticipantConfigItem.Object getObject()
            throws ConfigurationException {
        return child(
                value().getObject(),
                SimulatedReferenceTypeParticipantConfigItem.Object.class,
                SimulatedReferenceTypeDefinitionType.F_OBJECT);
    }


    public @NotNull AttributeBinding getPrimaryAttributeBinding() throws ConfigurationException {
        return new AttributeBinding(
                getSubject().getPrimaryBindingAttributeName(),
                getObject().getPrimaryBindingAttributeName());
    }

    public @Nullable AttributeBinding getSecondaryAttributeBinding() throws ConfigurationException {
        var subjectSide = getSubject().getSecondaryBindingAttributeName();
        var objectSide = getObject().getSecondaryBindingAttributeName();
        if (subjectSide != null && objectSide != null) {
            return new AttributeBinding(subjectSide, objectSide);
        } else if (subjectSide == null && objectSide == null) {
            return null;
        } else {
            throw configException("Secondary binding attribute is defined only on one side; in %s", DESC);
        }
    }


    public @NotNull ItemName getItemName() throws ConfigurationException {
        return ItemName.fromQName(
                getSubject().getLocalItemName());
    }

    public @NotNull ResourceObjectAssociationDirectionType getDirection() throws ConfigurationException {
        return nonNull(value().getDirection(), "association direction");
    }

    public boolean requiresExplicitReferentialIntegrity() {
        return BooleanUtils.isNotFalse(value().isExplicitReferentialIntegrity());
    }

    @Override
    public @NotNull String localDescription() {
        return "simulated reference type '%s' definition".formatted(value().getName());
    }
}
