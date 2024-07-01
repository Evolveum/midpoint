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
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.config.ResourceObjectAssociationConfigItem;
import com.evolveum.midpoint.schema.config.SimulatedReferenceTypeConfigItem;
import com.evolveum.midpoint.schema.config.SimulatedAssociationClassParticipantDelineationConfigItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;

/**
 * Specifies how to simulate the association class: what are the participants, what attributes to use for the association, etc.
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
     * The "referential" association object-side definition. It is used when we have no particular object in hand.
     * It must contain the definitions of all relevant object-side binding attributes (primary/secondary).
     * */
    @NotNull private final ResourceObjectDefinition referentialObjectDefinition;

    @NotNull private final ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition;

    /** Never empty. */
    @NotNull private final Collection<SimulatedAssociationClassParticipantDefinition> subjects;

    /** Never empty. */
    @NotNull private final Collection<SimulatedAssociationClassParticipantDefinition> objects;

    private SimulatedShadowReferenceTypeDefinition(
            @NotNull String associationClassName,
            @NotNull ItemName localSubjectItemName,
            @NotNull AttributeBinding primaryAttributeBinding,
            @Nullable AttributeBinding secondaryAttributeBinding,
            @Nullable QName primaryBindingMatchingRuleLegacy,
            @NotNull ResourceObjectAssociationDirectionType direction,
            boolean requiresExplicitReferentialIntegrity,
            @NotNull ResourceObjectDefinition referentialObjectDefinition,
            @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
            @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
            @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
        super(associationClassName, referentialObjectDefinition);
        this.localSubjectItemName = localSubjectItemName;
        this.primaryAttributeBinding = primaryAttributeBinding;
        this.secondaryAttributeBinding = secondaryAttributeBinding;
        this.primaryBindingMatchingRuleLegacy = primaryBindingMatchingRuleLegacy;
        this.direction = direction;
        this.requiresExplicitReferentialIntegrity = requiresExplicitReferentialIntegrity;
        this.subjectSidePrimaryBindingAttributeDefinition = subjectSidePrimaryBindingAttributeDefinition;
        this.referentialObjectDefinition = referentialObjectDefinition;
        this.subjects = MiscUtil.stateNonEmpty(subjects, "no subject definitions in %s", this);
        this.objects = MiscUtil.stateNonEmpty(objects, "no object definitions in %s", this);
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
            return referentialObjectDefinition.findSimpleAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @NotNull Collection<SimulatedAssociationClassParticipantDefinition> getSubjects() {
        return stateNonEmpty(
                subjects, "No subject delineations in %s (already checked!)", this);
    }

    public @NotNull Collection<SimulatedAssociationClassParticipantDefinition> getObjects() {
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
            Collection<SimulatedAssociationClassParticipantDefinition> definitions) {
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
        DebugUtil.debugDumpWithLabelLn(sb,"localSubjectItemName", localSubjectItemName, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb,"primaryAttributeBinding", primaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb,"secondaryAttributeBinding", secondaryAttributeBinding, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"primaryBindingMatchingRuleLegacy", primaryBindingMatchingRuleLegacy, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"direction", direction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"requiresExplicitReferentialIntegrity", requiresExplicitReferentialIntegrity, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb,"referentialObjectDefinition", referentialObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb,"subjectDelineations", subjects, indent + 1);
        DebugUtil.debugDumpWithLabel(sb,"objectDelineations", objects, indent + 1);
        return sb.toString();
    }

    /** Simulation as defined in pre-4.9 way (in "association" item in resource object type definition). */
    static class Legacy extends SimulatedShadowReferenceTypeDefinition {

        private Legacy(
                @NotNull String associationClassName,
                @NotNull ItemName localSubjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition referentialObjectDefinition,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
            super(associationClassName, localSubjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, referentialObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        /**
         * Note that the subject definition is currently being built. However, it should have all the attributes in place.
         */
        static @NotNull SimulatedShadowReferenceTypeDefinition parse(
                @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
                @NotNull ResourceSchemaImpl schemaBeingParsed,
                @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition,
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions)
                throws ConfigurationException {
            var primaryBinding = definitionCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

            var referentialObjectDefinition = objectTypeDefinitions.iterator().next();

            // This name is actually not important. It is not registered anywhere.
            String associationClassName = definitionCI.getItemName().getLocalPart();

            return new Legacy(
                    associationClassName,
                    definitionCI.getItemName(),
                    primaryBinding,
                    definitionCI.getSecondaryAttributeBinding(),
                    definitionCI.getMatchingRule(),
                    definitionCI.getDirection(),
                    definitionCI.isExplicitReferentialIntegrity(),
                    referentialObjectDefinition,
                    definitionCI.configNonNull(
                            referentialSubjectDefinition.findSimpleAttributeDefinition(subjectSidePrimaryBindingAttrName),
                            "No definition for subject-side primary binding attribute %s in %s",
                            subjectSidePrimaryBindingAttrName, DESC),
                    getLegacySubjectDefinitions(definitionCI, referentialSubjectDefinition),
                    getLegacyObjectDefinitions(definitionCI, schemaBeingParsed, referentialObjectDefinition));
        }

        private static @NotNull Collection<SimulatedAssociationClassParticipantDefinition> getLegacySubjectDefinitions(
                @NotNull ResourceObjectAssociationConfigItem definitionCI,
                @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition) {
            return List.of(
                    SimulatedAssociationClassParticipantDefinition.fromObjectTypeDefinition(
                            referentialSubjectDefinition,
                            definitionCI.value().getAuxiliaryObjectClass()));
        }

        /** Always non-empty. */
        private static Collection<SimulatedAssociationClassParticipantDefinition> getLegacyObjectDefinitions(
                @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
                @NotNull ResourceSchema resourceSchema,
                @NotNull ResourceObjectTypeDefinition referentialObjectDefinition)
                throws ConfigurationException {
            var kind = definitionCI.getKind();
            // There must be at least one intent. If none is provided, we derive it from the object definition.
            var configuredIntents = definitionCI.getIntents();
            var realIntents = configuredIntents.isEmpty() ? List.of(referentialObjectDefinition.getIntent()) : configuredIntents;

            List<SimulatedAssociationClassParticipantDefinition> definitions = new ArrayList<>();
            for (String intent : realIntents) {
                var typeIdentification = ResourceObjectTypeIdentification.of(kind, intent);
                var typeDef = definitionCI.configNonNull(
                        resourceSchema.getObjectTypeDefinition(typeIdentification),
                        "No object type definition for kind %s and intent %s in %s", kind, intent, DESC);
                definitions.add(
                        new SimulatedAssociationClassParticipantDefinition(
                                typeDef.getTypeName(),
                                typeDef.getDelineation().getBaseContext(),
                                typeDef.getDelineation().getSearchHierarchyScope(),
                                ShadowRelationParticipantType.forObjectType(typeDef),
                                null));
            }
            return definitions;
        }

        @Override
        public boolean isLegacy() {
            return true;
        }
    }

    static class Modern
            extends SimulatedShadowReferenceTypeDefinition {

        private Modern(
                @NotNull String associationClassLocalName,
                @NotNull ItemName localSubjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition referentialObjectDefinition,
                @NotNull ShadowSimpleAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
            super(associationClassLocalName, localSubjectItemName,
                    primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, referentialObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        static Modern parse(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl schemaBeingParsed) throws ConfigurationException {

            var primaryBinding = simulationCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

            // TODO check the compatibility of subject/object definitions!

            var subjects = getSubjectDefinitions(simulationCI, schemaBeingParsed);
            var objects = getObjectDefinitions(simulationCI, schemaBeingParsed);

            // The "referential" association subject-side definition. It must contain the definitions of all relevant
            // subject-side binding attributes (primary/secondary). Hence, it should contain the subject's auxiliary object class
            // definition, if present.
            var referentialSubjectDefinition = subjects.iterator().next().getObjectDefinition();
            var referentialObjectDefinition = objects.iterator().next().getObjectDefinition();

            return new Modern(
                    simulationCI.getNameLocalPart(),
                    simulationCI.getItemName(),
                    primaryBinding,
                    simulationCI.getSecondaryAttributeBinding(),
                    null,
                    simulationCI.getDirection(),
                    simulationCI.requiresExplicitReferentialIntegrity(),
                    referentialObjectDefinition,
                    simulationCI.configNonNull(
                            referentialSubjectDefinition.findSimpleAttributeDefinition(subjectSidePrimaryBindingAttrName),
                            "No definition for subject-side primary binding attribute %s in %s",
                            subjectSidePrimaryBindingAttrName, DESC),
                    subjects,
                    objects);
        }

        private static Collection<SimulatedAssociationClassParticipantDefinition> getSubjectDefinitions(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            var delineationCIs = simulationCI.getSubject().getDelineations();
            return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema);
        }

        private static Collection<SimulatedAssociationClassParticipantDefinition> getObjectDefinitions(
                @NotNull SimulatedReferenceTypeConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            var delineationCIs = simulationCI.getObject().getDelineations();
            return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema);
        }

        private @NotNull static List<SimulatedAssociationClassParticipantDefinition> definitionsFromSimulationConfiguration(
                List<SimulatedAssociationClassParticipantDelineationConfigItem> delineationCIs,
                @NotNull ResourceSchemaImpl resourceSchema) throws ConfigurationException {
            argCheck(!delineationCIs.isEmpty(), "No delineations (already checked)");
            List<SimulatedAssociationClassParticipantDefinition> definitions = new ArrayList<>();
            for (var delineationCI : delineationCIs) {
                QName objectClassName = delineationCI.getObjectClassName();
                definitions.add(
                        new SimulatedAssociationClassParticipantDefinition(
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
}
