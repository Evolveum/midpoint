/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchemaImpl;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.SimulatedAssociationClassParticipantDelineation;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedAssociationClassType;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

public class SimulatedAssociationClassConfigItem
        extends ConfigurationItem<SimulatedAssociationClassType> {

    @SuppressWarnings("unused") // called dynamically
    public SimulatedAssociationClassConfigItem(@NotNull ConfigurationItem<SimulatedAssociationClassType> original) {
        super(original);
    }

    public @NotNull QName getName() throws ConfigurationException {
        return QNameUtil.qualifyIfNeeded(
                nonNull(value().getName(), "association class name"),
                MidPointConstants.NS_RI);
    }

    public @NotNull SimulatedAssociationClassParticipantConfigItem.Subject getSubject()
            throws ConfigurationException {
        return child(
                value().getSubject(),
                SimulatedAssociationClassParticipantConfigItem.Subject.class,
                SimulatedAssociationClassType.F_SUBJECT);
    }

    public @NotNull SimulatedAssociationClassParticipantConfigItem.Object getObject()
            throws ConfigurationException {
        return child(
                value().getObject(),
                SimulatedAssociationClassParticipantConfigItem.Object.class,
                SimulatedAssociationClassType.F_OBJECT);
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

    public Collection<SimulatedAssociationClassParticipantDelineation> getSubjectDelineations(
            @NotNull CompleteResourceSchemaImpl resourceSchema,
            @NotNull Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions) throws ConfigurationException {
        List<SimulatedAssociationClassParticipantDelineationConfigItem> delineationCIs = getSubject().getDelineations();
        if (delineationCIs.isEmpty()) {
            argCheck(!subjectTypeDefinitions.isEmpty(), "no subject type definitions (already checked)");
            return subjectTypeDefinitions.stream()
                    .map(def ->
                            SimulatedAssociationClassParticipantDelineation.fromObjectTypeDefinition(def, null))
                    .toList();
        } else {
            List<SimulatedAssociationClassParticipantDelineation> delineations = new ArrayList<>();
            for (var delineationCI : delineationCIs) {
                QName objectClassName = delineationCI.getObjectClassName();
                delineations.add(
                        new SimulatedAssociationClassParticipantDelineation(
                                objectClassName,
                                delineationCI.getBaseContext(),
                                delineationCI.getSearchHierarchyScope(),
                                delineationCI.configNonNull(
                                        resourceSchema.findDefinitionForObjectClass(objectClassName),
                                        "No definition for object class %s found in %s", objectClassName, DESC),
                                delineationCI.getAuxiliaryObjectClassName()));
            }
            return delineations;
        }
    }

    public Collection<SimulatedAssociationClassParticipantDelineation> getObjectDelineations(
            CompleteResourceSchemaImpl resourceSchema,
            ShadowAssociationTypeDefinitionConfigItem definitionCI) throws ConfigurationException {
        List<SimulatedAssociationClassParticipantDelineationConfigItem> delineationCIs = getObject().getDelineations();
        List<SimulatedAssociationClassParticipantDelineation> delineations = new ArrayList<>();
        if (delineationCIs.isEmpty()) {
            for (ResourceObjectTypeIdentification objectTypeIdentification : definitionCI.getObjectTypeIdentifiers()) {
                var objectDef = configNonNull(
                        resourceSchema.getObjectTypeDefinition(objectTypeIdentification),
                        "No object type definition found for %s used in %s", objectTypeIdentification, DESC);
                delineations.add(
                        SimulatedAssociationClassParticipantDelineation.fromObjectTypeDefinition(objectDef, null));
            }
        } else { // TODO deduplicate with the method above
            for (var delineationCI : delineationCIs) {
                QName objectClassName = delineationCI.getObjectClassName();
                delineations.add(
                        new SimulatedAssociationClassParticipantDelineation(
                                objectClassName,
                                delineationCI.getBaseContext(),
                                delineationCI.getSearchHierarchyScope(),
                                delineationCI.configNonNull(
                                        resourceSchema.findDefinitionForObjectClass(objectClassName),
                                        "No definition for object class %s found in %s", objectClassName, DESC),
                                delineationCI.getAuxiliaryObjectClassName()));
            }
        }
        return delineations;
    }

    @Override
    public @NotNull String localDescription() {
        return "simulated association class '%s' definition".formatted(value().getName());
    }
}
