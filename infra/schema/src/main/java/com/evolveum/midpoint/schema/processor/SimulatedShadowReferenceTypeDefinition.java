/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.config.ResourceObjectAssociationConfigItem;
import com.evolveum.midpoint.schema.config.SimulatedReferenceTypeConfigItem;
import com.evolveum.midpoint.schema.config.SimulatedReferenceTypeParticipantDelineationConfigItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;

/**
 * Specifies how to simulate the reference type: what are the participants, what attributes to use for the reference, etc.
 */
public abstract class SimulatedShadowReferenceTypeDefinition
        extends AbstractShadowReferenceTypeDefinition
        implements Serializable, DebugDumpable {

    @NotNull private final ItemName localSubjectItemName;

    @NotNull private final AttributeBinding primaryAttributeBinding;

    @Nullable private final AttributeBinding secondaryAttributeBinding;

    @Nullable private final QName primaryBindingMatchingRuleLegacy;

    @NotNull private final ResourceObjectAssociationDirectionType direction;

    private final boolean requiresExplicitReferentialIntegrity;

    @NotNull private final ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition;

    /** Never empty. Immutable. */
    @NotNull private final Collection<SimulatedReferenceTypeParticipantDefinition> subjects;

    /** Never empty. Immutable. */
    @NotNull private final Collection<SimulatedReferenceTypeParticipantDefinition> objects;

    private SimulatedShadowReferenceTypeDefinition(
            @NotNull String referenceTypeLocalName,
            @NotNull ItemName localSubjectItemName,
            @NotNull AttributeBinding primaryAttributeBinding,
            @Nullable AttributeBinding secondaryAttributeBinding,
            @Nullable QName primaryBindingMatchingRuleLegacy,
            @NotNull ResourceObjectAssociationDirectionType direction,
            boolean requiresExplicitReferentialIntegrity,
            @NotNull ResourceObjectDefinition generalizedObjectSideObjectDefinition,
            @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
            @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
            @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
        super(referenceTypeLocalName, generalizedObjectSideObjectDefinition);
        this.localSubjectItemName = localSubjectItemName;
        this.primaryAttributeBinding = primaryAttributeBinding;
        this.secondaryAttributeBinding = secondaryAttributeBinding;
        this.primaryBindingMatchingRuleLegacy = primaryBindingMatchingRuleLegacy;
        this.direction = direction;
        this.requiresExplicitReferentialIntegrity = requiresExplicitReferentialIntegrity;
        this.subjectSidePrimaryBindingAttributeDefinition = subjectSidePrimaryBindingAttributeDefinition;
        this.subjects = List.copyOf(MiscUtil.stateNonEmpty(subjects, "no subject definitions in %s", this));
        this.objects = List.copyOf(MiscUtil.stateNonEmpty(objects, "no object definitions in %s", this));
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

    /** Returns the definition of the binding attribute without knowing what particular object is involved. */
    public <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(AttributeBinding binding) {
        try {
            return getGeneralizedObjectSideObjectDefinition().findSimpleAttributeDefinitionRequired(binding.objectSide());
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> getSubjects() {
        return stateNonEmpty(
                subjects, "No subject delineations in %s (already checked!)", this);
    }

    public @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> getObjects() {
        return stateNonEmpty(
                objects, "No object delineations in %s (already checked!)", this);
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

    public @NotNull ShadowSimpleAttributeDefinition<?> getSubjectSidePrimaryBindingAttributeDef() {
        return subjectSidePrimaryBindingAttributeDefinition;
    }

    public @Nullable QName getPrimaryBindingMatchingRuleLegacy() {
        return primaryBindingMatchingRuleLegacy;
    }

    @Override
    public @NotNull Collection<ShadowRelationParticipantType> getSubjectTypes() {
        return toParticipants(subjects);
    }

    @Override
    public @NotNull Collection<ShadowRelationParticipantType> getObjectTypes() {
        return toParticipants(objects);
    }

    private @NotNull Collection<ShadowRelationParticipantType> toParticipants(
            Collection<SimulatedReferenceTypeParticipantDefinition> definitions) {
        return definitions.stream()
                .map(def -> def.getParticipantType())
                .toList();
    }

    boolean isRelevantForSubject(@NotNull ResourceObjectDefinition definition) {
        return getSubjects().stream().anyMatch(
                subject -> subject.matches(definition));
    }

    public abstract boolean isLegacy();

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "localSubjectItemName", localSubjectItemName, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb, "primaryAttributeBinding", primaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb, "secondaryAttributeBinding", secondaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryBindingMatchingRuleLegacy", primaryBindingMatchingRuleLegacy, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "direction", direction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "requiresExplicitReferentialIntegrity", requiresExplicitReferentialIntegrity, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "generalizedObjectSideObjectDefinition", getGeneralizedObjectSideObjectDefinition(), indent + 1); // consider removing
        DebugUtil.debugDumpWithLabelLn(sb, "subjectDelineations", subjects, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "objectDelineations", objects, indent + 1);
        return sb.toString();
    }

    /** Simulation as defined in pre-4.9 way (in "association" item in resource object type definition). */
    static class Legacy extends SimulatedShadowReferenceTypeDefinition {

        private Legacy(
                @NotNull String referenceTypeLocalName,
                @NotNull ItemName subjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition generalizedObjectSideObjectDefinition,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
            super(referenceTypeLocalName, subjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, generalizedObjectSideObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        /**
         * Note that the subject definition is currently being built. However, it should have all the attributes in place.
         */
        static @NotNull SimulatedShadowReferenceTypeDefinition parse(
                @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
                @NotNull ResourceObjectDefinition subjectDefinition,
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions,
                @NotNull ResourceSchema schemaBeingParsed)
                throws ConfigurationException {

            // The subject definition is fixed, because the legacy specification is always attached to given SINGLE object type.
            var primaryBinding = definitionCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();
            var subjectSidePrimaryBindingAttrDef = definitionCI.configNonNull(
                    subjectDefinition.findSimpleAttributeDefinition(subjectSidePrimaryBindingAttrName),
                    "No definition for subject-side primary binding attribute %s in %s",
                    subjectSidePrimaryBindingAttrName, DESC);

            // This name is actually not important. It is not registered anywhere.
            String referenceTypeLocalName = definitionCI.getItemName().getLocalPart();

            var generalizedObjectSideObjectDefinition = computeGeneralizedObjectSideObjectDefinition(
                    objectTypeDefinitions, List.of(), definitionCI, schemaBeingParsed);

            return new Legacy(
                    referenceTypeLocalName,
                    definitionCI.getItemName(),
                    primaryBinding,
                    definitionCI.getSecondaryAttributeBinding(),
                    definitionCI.getMatchingRule(),
                    definitionCI.getDirection(),
                    definitionCI.isExplicitReferentialIntegrity(),
                    generalizedObjectSideObjectDefinition,
                    subjectSidePrimaryBindingAttrDef,
                    getLegacySubjectDefinitions(definitionCI, subjectDefinition, schemaBeingParsed),
                    getLegacyObjectDefinitions(objectTypeDefinitions, schemaBeingParsed));
        }

        private static @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> getLegacySubjectDefinitions(
                @NotNull ResourceObjectAssociationConfigItem definitionCI,
                @NotNull ResourceObjectDefinition subjectDefinition,
                @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
            return List.of(
                    SimulatedReferenceTypeParticipantDefinition.fromObjectTypeOrClassDefinition(
                            subjectDefinition,
                            definitionCI.value().getAuxiliaryObjectClass(),
                            resourceSchema));
        }

        /** Returns object definitions for this legacy associations. (Always non-empty.) */
        private static Collection<SimulatedReferenceTypeParticipantDefinition> getLegacyObjectDefinitions(
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions,
                @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
            List<SimulatedReferenceTypeParticipantDefinition> definitions = new ArrayList<>();
            for (var objectTypeDefinition : objectTypeDefinitions) {
                definitions.add(
                        SimulatedReferenceTypeParticipantDefinition.fromObjectTypeOrClassDefinition(
                                objectTypeDefinition,
                                null,
                                resourceSchema));
            }
            return definitions;
        }

        @Override
        public boolean isLegacy() {
            return true;
        }
    }

    static class Modern extends SimulatedShadowReferenceTypeDefinition {

        private Modern(
                @NotNull String referenceTypeLocalName,
                @NotNull ItemName localSubjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition generalizedObjectSideObjectDefinition,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
            super(referenceTypeLocalName, localSubjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, generalizedObjectSideObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        static Modern parse(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl schemaBeingParsed) throws ConfigurationException {

            var primaryBinding = simulationCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

            // The "referential" subject-side definition. It must contain the definitions of all relevant subject-side binding
            // attributes (primary/secondary). Hence, it should contain the subject's auxiliary object class definition, if
            // present.
            // TODO check the compatibility of these subject-side definitions!
            var subjects = getSubjectDefinitions(simulationCI, schemaBeingParsed);
            var referentialSubjectDefinition = subjects.iterator().next().getObjectDefinition();
            var subjectSidePrimaryBindingAttrDef = simulationCI.configNonNull(
                    referentialSubjectDefinition.findSimpleAttributeDefinition(subjectSidePrimaryBindingAttrName),
                    "No definition for subject-side primary binding attribute %s in %s",
                    subjectSidePrimaryBindingAttrName, DESC);

            var objects = getObjectDefinitions(simulationCI, schemaBeingParsed);
            var objectDefinitions = objects.stream()
                    .map(SimulatedReferenceTypeParticipantDefinition::getObjectDefinition)
                    .toList();
            var extraAuxiliaryObjectClassNames = objects.stream()
                    .map(SimulatedReferenceTypeParticipantDefinition::getAuxiliaryObjectClassName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            var generalizedObjectSideObjectDefinition = computeGeneralizedObjectSideObjectDefinition(
                    objectDefinitions, extraAuxiliaryObjectClassNames, simulationCI, schemaBeingParsed);

            return new Modern(
                    simulationCI.getNameLocalPart(),
                    simulationCI.getItemName(),
                    primaryBinding,
                    simulationCI.getSecondaryAttributeBinding(),
                    null,
                    simulationCI.getDirection(),
                    simulationCI.requiresExplicitReferentialIntegrity(),
                    generalizedObjectSideObjectDefinition,
                    subjectSidePrimaryBindingAttrDef,
                    subjects,
                    objects);
        }

        private static Collection<SimulatedReferenceTypeParticipantDefinition> getSubjectDefinitions(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            var delineationCIs = simulationCI.getSubject().getDelineations();
            return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema);
        }

        private static Collection<SimulatedReferenceTypeParticipantDefinition> getObjectDefinitions(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            var delineationCIs = simulationCI.getObject().getDelineations();
            return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema);
        }

        private @NotNull static List<SimulatedReferenceTypeParticipantDefinition> definitionsFromSimulationConfiguration(
                List<SimulatedReferenceTypeParticipantDelineationConfigItem> delineationCIs,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            argCheck(!delineationCIs.isEmpty(), "No delineations (already checked)");
            List<SimulatedReferenceTypeParticipantDefinition> definitions = new ArrayList<>();
            for (var delineationCI : delineationCIs) {
                QName objectClassName = delineationCI.getObjectClassName();
                definitions.add(
                        new SimulatedReferenceTypeParticipantDefinition(
                                objectClassName,
                                delineationCI.getBaseContext(),
                                delineationCI.getSearchHierarchyScope(),
                                ShadowRelationParticipantType.forObjectClass(
                                        delineationCI.configNonNull(
                                                resourceSchema.findDefinitionForObjectClass(objectClassName),
                                                "No definition for object class %s found in %s", objectClassName, DESC)),
                                delineationCI.getAuxiliaryObjectClassName(),
                                resourceSchema));
            }
            return definitions;
        }

        @Override
        public boolean isLegacy() {
            return false;
        }
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        subjects.forEach(SimulatedReferenceTypeParticipantDefinition::freeze);
        objects.forEach(SimulatedReferenceTypeParticipantDefinition::freeze);
    }
}
