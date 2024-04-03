/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.config.ResourceObjectAssociationConfigItem;
import com.evolveum.midpoint.schema.config.ShadowAssociationTypeDefinitionConfigItem;
import com.evolveum.midpoint.schema.config.SimulatedAssociationClassConfigItem;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.util.MiscUtil.stateNonEmpty;

/**
 * Specifies how to simulate the association class, e.g., what attributes to use for the association.
 */
public class ShadowAssociationClassSimulationDefinition implements Serializable, DebugDumpable {

    @NotNull private final ItemName localSubjectItemName;

    @NotNull private final AttributeBinding primaryAttributeBinding;

    @Nullable private final AttributeBinding secondaryAttributeBinding;

    @Nullable private final QName primaryBindingMatchingRuleLegacy;

    @NotNull private final ResourceObjectAssociationDirectionType direction;

    private final boolean requiresExplicitReferentialIntegrity;

    /**
     * The "referential" association object-side definition. It is used when we have no particular object in hand.
     * It must contain the definitions of all relevant object-side binding attributes (primary/secondary).
     * */
    @NotNull private final ResourceObjectDefinition referentialObjectDefinition;

    @NotNull private final ResourceAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition;

    /** Never empty. */
    @NotNull private final Collection<SimulatedAssociationClassParticipantDelineation> subjectDelineations;

    /** Never empty. */
    @NotNull private final Collection<SimulatedAssociationClassParticipantDelineation> objectDelineations;

    private ShadowAssociationClassSimulationDefinition(
            @NotNull ItemName localSubjectItemName,
            @NotNull AttributeBinding primaryAttributeBinding,
            @Nullable AttributeBinding secondaryAttributeBinding,
            @Nullable QName primaryBindingMatchingRuleLegacy,
            @NotNull ResourceObjectAssociationDirectionType direction,
            boolean requiresExplicitReferentialIntegrity,
            @NotNull ResourceObjectDefinition referentialObjectDefinition,
            @NotNull ResourceAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
            @NotNull Collection<SimulatedAssociationClassParticipantDelineation> subjectDelineations,
            @NotNull Collection<SimulatedAssociationClassParticipantDelineation> objectDelineations) {
        this.localSubjectItemName = localSubjectItemName;
        this.primaryAttributeBinding = primaryAttributeBinding;
        this.secondaryAttributeBinding = secondaryAttributeBinding;
        this.primaryBindingMatchingRuleLegacy = primaryBindingMatchingRuleLegacy;
        this.direction = direction;
        this.requiresExplicitReferentialIntegrity = requiresExplicitReferentialIntegrity;
        this.subjectSidePrimaryBindingAttributeDefinition = subjectSidePrimaryBindingAttributeDefinition;
        this.referentialObjectDefinition = referentialObjectDefinition;
        this.subjectDelineations = MiscUtil.stateNonEmpty(subjectDelineations, "no subject delineations in %s", this);
        this.objectDelineations = MiscUtil.stateNonEmpty(objectDelineations, "no object delineations in %s", this);
    }

    static ShadowAssociationClassSimulationDefinition parseModern(
            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI,
            @NotNull SimulatedAssociationClassConfigItem simulationCI,
            @NotNull CompleteResourceSchemaImpl schemaBeingParsed,
            @NotNull Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) throws ConfigurationException {

        var primaryBinding = simulationCI.getPrimaryAttributeBinding();
        var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

        // TODO check the compatibility of subject/object definitions!

        // The "referential" association subject-side definition. It must contain the definitions of all relevant
        // subject-side binding attributes (primary/secondary). Hence, it should contain the subject's auxiliary object class
        // definition, if present.
        var referentialSubjectDefinition = subjectTypeDefinitions.iterator().next();
        var referentialObjectDefinition = objectTypeDefinitions.iterator().next();

        return new ShadowAssociationClassSimulationDefinition(
                simulationCI.getItemName(),
                primaryBinding,
                simulationCI.getSecondaryAttributeBinding(),
                null,
                simulationCI.getDirection(),
                simulationCI.requiresExplicitReferentialIntegrity(),
                referentialObjectDefinition,
                definitionCI.configNonNull(
                        referentialSubjectDefinition.findAttributeDefinition(subjectSidePrimaryBindingAttrName),
                        "No definition for subject-side primary binding attribute %s in %s",
                        subjectSidePrimaryBindingAttrName, DESC),
                simulationCI.getSubjectDelineations(schemaBeingParsed, subjectTypeDefinitions),
                simulationCI.getObjectDelineations(schemaBeingParsed, definitionCI));
    }

    /**
     * Note that the subject definition is currently being built. However, it should have all the attributes in place.
     */
    static ShadowAssociationClassSimulationDefinition parseLegacy(
            @NotNull ResourceObjectAssociationConfigItem definitionCI,
            @NotNull CompleteResourceSchemaImpl schemaBeingParsed,
            @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions)
            throws ConfigurationException {
        var primaryBinding = definitionCI.getPrimaryAttributeBinding();
        var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

        var referentialObjectDefinition = objectTypeDefinitions.iterator().next();

        return new ShadowAssociationClassSimulationDefinition(
                definitionCI.getItemName(),
                primaryBinding,
                definitionCI.getSecondaryAttributeBinding(),
                definitionCI.getMatchingRule(),
                definitionCI.getDirection(),
                definitionCI.isExplicitReferentialIntegrity(),
                referentialObjectDefinition,
                definitionCI.configNonNull(
                        referentialSubjectDefinition.findAttributeDefinition(subjectSidePrimaryBindingAttrName),
                        "No definition for subject-side primary binding attribute %s in %s",
                        subjectSidePrimaryBindingAttrName, DESC),
                definitionCI.getSubjectDelineations(referentialSubjectDefinition),
                definitionCI.getObjectDelineations(schemaBeingParsed, referentialObjectDefinition));
    }

    public @NotNull ItemName getLocalSubjectItemName() {
        return localSubjectItemName;
    }

    public @NotNull AttributeBinding getPrimaryAttributeBinding() {
        return primaryAttributeBinding;
    }

    public @Nullable AttributeBinding getSecondaryAttributeBinding() {
        return secondaryAttributeBinding;
    }

    public @NotNull ResourceObjectAssociationDirectionType getDirection() {
        return direction;
    }

    public boolean isObjectToSubject() {
        return direction == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;
    }

    public boolean isSubjectToObject() {
        return direction == ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT;
    }

    public boolean requiresExplicitReferentialIntegrity() {
        return requiresExplicitReferentialIntegrity;
    }

    public <T> ResourceAttributeDefinition<T> getObjectAttributeDefinition(AttributeBinding binding) {
        return getObjectAttributeDefinition(binding.objectSide());
    }

    private <T> ResourceAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return referentialObjectDefinition.findAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @NotNull Collection<SimulatedAssociationClassParticipantDelineation> getSubjectDelineations() {
        return stateNonEmpty(
                subjectDelineations, "No subject delineations in %s (already checked!)", this);
    }

    public @NotNull Collection<SimulatedAssociationClassParticipantDelineation> getObjectDelineations() {
        return stateNonEmpty(
                objectDelineations, "No object delineations in %s (already checked!)", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "localSubjectItemName=" + localSubjectItemName +
                ", primaryBinding=" + primaryAttributeBinding +
                ", secondaryBinding=" + secondaryAttributeBinding +
                '}'; // TODO other parts?
    }

    public QName getPrimaryObjectBindingAttributeName() {
        return primaryAttributeBinding.objectSide();
    }

    public QName getPrimarySubjectBindingAttributeName() {
        return primaryAttributeBinding.subjectSide();
    }

    public @NotNull ResourceAttributeDefinition<?> getSubjectSidePrimaryBindingAttributeDef() {
        return subjectSidePrimaryBindingAttributeDefinition;
    }

    public @Nullable QName getPrimaryBindingMatchingRuleLegacy() {
        return primaryBindingMatchingRuleLegacy;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb,"localSubjectItemName", localSubjectItemName, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb,"primaryAttributeBinding", primaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb,"secondaryAttributeBinding", secondaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"primaryBindingMatchingRuleLegacy", primaryBindingMatchingRuleLegacy, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"direction", direction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"requiresExplicitReferentialIntegrity", requiresExplicitReferentialIntegrity, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb,"referentialObjectDefinition", referentialObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"subjectDelineations", subjectDelineations, indent + 1);
        DebugUtil.debugDumpWithLabel(sb,"objectDelineations", objectDelineations, indent + 1);
        return sb.toString();
    }
}
