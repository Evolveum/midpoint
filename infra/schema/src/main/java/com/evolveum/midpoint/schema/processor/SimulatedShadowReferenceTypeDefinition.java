/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.config.ConfigurationItem;

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
 *
 * Immutable after the containing resource is frozen.
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

    /**
     * Definition for the object class of the target (referenced) object.
     * It must contain the definitions of all relevant object-side binding attributes (primary/secondary).
     */
    @NotNull private final ResourceObjectDefinition definitionForTargetObjectClass;

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
            @NotNull ResourceObjectDefinition definitionForTargetObjectClass,
            @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
            @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
            @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
        super(referenceTypeLocalName);
        this.localSubjectItemName = localSubjectItemName;
        this.primaryAttributeBinding = primaryAttributeBinding;
        this.secondaryAttributeBinding = secondaryAttributeBinding;
        this.primaryBindingMatchingRuleLegacy = primaryBindingMatchingRuleLegacy;
        this.direction = direction;
        this.requiresExplicitReferentialIntegrity = requiresExplicitReferentialIntegrity;
        this.subjectSidePrimaryBindingAttributeDefinition = subjectSidePrimaryBindingAttributeDefinition;
        this.definitionForTargetObjectClass = definitionForTargetObjectClass;
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

    public <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(AttributeBinding binding) {
        return getObjectAttributeDefinition(binding.objectSide());
    }

    private <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return definitionForTargetObjectClass.findSimpleAttributeDefinitionRequired(attrName);
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
        DebugUtil.debugDumpWithLabelToStringLn(sb, "definitionForTargetObjectClass", definitionForTargetObjectClass, indent + 1);
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
                @NotNull ResourceObjectDefinition definitionForTargetObjectClass,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
            super(referenceTypeLocalName, subjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, definitionForTargetObjectClass,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        /**
         * Note that the subject definition is currently being built. However, it should have all the attributes in place.
         */
        static @NotNull SimulatedShadowReferenceTypeDefinition parse(
                @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
                @NotNull ResourceObjectDefinition subjectDefinition,
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions)
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

            return new Legacy(
                    referenceTypeLocalName,
                    definitionCI.getItemName(),
                    primaryBinding,
                    definitionCI.getSecondaryAttributeBinding(),
                    definitionCI.getMatchingRule(),
                    definitionCI.getDirection(),
                    definitionCI.isExplicitReferentialIntegrity(),
                    getDefinitionForTargetObjectClass(objectTypeDefinitions, definitionCI),
                    subjectSidePrimaryBindingAttrDef,
                    getLegacySubjectDefinitions(definitionCI, subjectDefinition),
                    getLegacyObjectDefinitions(objectTypeDefinitions));
        }

        private static @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> getLegacySubjectDefinitions(
                @NotNull ResourceObjectAssociationConfigItem definitionCI,
                @NotNull ResourceObjectDefinition referentialSubjectDefinition) {
            return List.of(
                    SimulatedReferenceTypeParticipantDefinition.fromObjectDefinition(
                            referentialSubjectDefinition,
                            definitionCI.value().getAuxiliaryObjectClass()));
        }

        /** Returns object definitions for this legacy associations. (Always non-empty.) */
        private static Collection<SimulatedReferenceTypeParticipantDefinition> getLegacyObjectDefinitions(
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) {

            List<SimulatedReferenceTypeParticipantDefinition> definitions = new ArrayList<>();
            for (var objectTypeDefinition : objectTypeDefinitions) {
                definitions.add(
                        new SimulatedReferenceTypeParticipantDefinition(
                                objectTypeDefinition.getTypeName(),
                                objectTypeDefinition.getDelineation().getBaseContext(),
                                objectTypeDefinition.getDelineation().getSearchHierarchyScope(),
                                ShadowRelationParticipantType.forObjectType(objectTypeDefinition),
                                null));
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
                @NotNull ResourceObjectDefinition definitionForTargetObjectClass,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> subjects,
                @NotNull Collection<SimulatedReferenceTypeParticipantDefinition> objects) {
            super(referenceTypeLocalName, localSubjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, definitionForTargetObjectClass,
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

            return new Modern(
                    simulationCI.getNameLocalPart(),
                    simulationCI.getItemName(),
                    primaryBinding,
                    simulationCI.getSecondaryAttributeBinding(),
                    null,
                    simulationCI.getDirection(),
                    simulationCI.requiresExplicitReferentialIntegrity(),
                    getDefinitionForTargetObjectClass(objectDefinitions, simulationCI),
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
                                delineationCI.getAuxiliaryObjectClassName()));
            }
            return definitions;
        }

        @Override
        public boolean isLegacy() {
            return false;
        }
    }

    private static @NotNull ResourceObjectDefinition getDefinitionForTargetObjectClass(
            Collection<? extends ResourceObjectDefinition> objects, ConfigurationItem<?> containingCI) {
        var classDefinitions = objects.stream()
                .map(objectDefinition -> objectDefinition.getObjectClassDefinition())
                .collect(Collectors.toSet());
        var classDefinition = MiscUtil.extractSingletonRequired(
                classDefinitions,
                () -> new IllegalStateException(
                        "Multiple object class definitions in " + containingCI.fullDescription() + ": " + classDefinitions),
                () -> new IllegalStateException(
                        "No object class definition in " + containingCI.fullDescription()));
        return classDefinition.getEffectiveDefinition();
    }
}
