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
import com.evolveum.midpoint.schema.config.SimulatedAssociationClassParticipantDelineationConfigItem;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * Specifies how to simulate the association class: what are the participants, what attributes to use for the association, etc.
 */
public abstract class ShadowAssociationClassSimulationDefinition
        implements ShadowAssociationClassImplementation, Serializable, DebugDumpable {

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
    @NotNull private final Collection<SimulatedAssociationClassParticipantDefinition> subjects;

    /** Never empty. */
    @NotNull private final Collection<SimulatedAssociationClassParticipantDefinition> objects;

    private ShadowAssociationClassSimulationDefinition(
            @NotNull ItemName localSubjectItemName,
            @NotNull AttributeBinding primaryAttributeBinding,
            @Nullable AttributeBinding secondaryAttributeBinding,
            @Nullable QName primaryBindingMatchingRuleLegacy,
            @NotNull ResourceObjectAssociationDirectionType direction,
            boolean requiresExplicitReferentialIntegrity,
            @NotNull ResourceObjectDefinition referentialObjectDefinition,
            @NotNull ResourceAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
            @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
            @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
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

    public @NotNull ResourceAttributeDefinition<?> getSubjectSidePrimaryBindingAttributeDef() {
        return subjectSidePrimaryBindingAttributeDefinition;
    }

    public @Nullable QName getPrimaryBindingMatchingRuleLegacy() {
        return primaryBindingMatchingRuleLegacy;
    }

    @Override
    public @NotNull Collection<ShadowAssociationClassDefinition.Participant> getParticipatingSubjects() {
        return toParticipants(subjects);
    }

    @Override
    public @NotNull Collection<ShadowAssociationClassDefinition.Participant> getParticipatingObjects() {
        return toParticipants(objects);
    }

    private @NotNull Collection<ShadowAssociationClassDefinition.Participant> toParticipants(
            Collection<SimulatedAssociationClassParticipantDefinition> definitions) {
        return definitions.stream()
                .map(def -> new ShadowAssociationClassDefinition.Participant(def.getObjectDefinition(), def.getAssociationItemName()))
                .toList();
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
        DebugUtil.debugDumpWithLabelLn(sb,"subjectDelineations", subjects, indent + 1);
        DebugUtil.debugDumpWithLabel(sb,"objectDelineations", objects, indent + 1);
        return sb.toString();
    }

    /** Simulation as defined in pre-4.9 way (in "association" item in resource object type definition). */
    static class Legacy extends ShadowAssociationClassSimulationDefinition {

        private Legacy(
                @NotNull ItemName localSubjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition referentialObjectDefinition,
                @NotNull ResourceAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
            super(localSubjectItemName, primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, referentialObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
        }

        /**
         * Note that the subject definition is currently being built. However, it should have all the attributes in place.
         */
        static @NotNull ShadowAssociationClassSimulationDefinition parse(
                @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
                @NotNull ResourceSchemaImpl schemaBeingParsed,
                @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition,
                @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions)
                throws ConfigurationException {
            var primaryBinding = definitionCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

            var referentialObjectDefinition = objectTypeDefinitions.iterator().next();

            return new Legacy(
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
                    getLegacySubjectDefinitions(definitionCI, referentialSubjectDefinition),
                    getLegacyObjectDefinitions(definitionCI, schemaBeingParsed, referentialObjectDefinition));
        }

        private static @NotNull Collection<SimulatedAssociationClassParticipantDefinition> getLegacySubjectDefinitions(
                @NotNull ResourceObjectAssociationConfigItem definitionCI,
                @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition) throws ConfigurationException {
            return List.of(
                    SimulatedAssociationClassParticipantDefinition.fromObjectTypeDefinition(
                            referentialSubjectDefinition,
                            definitionCI.value().getAuxiliaryObjectClass(),
                            definitionCI.getItemName()));
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
                var typeDef = definitionCI.configNonNull(
                        resourceSchema.getObjectTypeDefinition(kind, intent),
                        "No object type definition for kind %s and intent %s in %s", kind, intent, DESC);
                definitions.add(
                        new SimulatedAssociationClassParticipantDefinition(
                                typeDef.getTypeName(),
                                typeDef.getDelineation().getBaseContext(),
                                typeDef.getDelineation().getSearchHierarchyScope(),
                                typeDef,
                                null,
                                null));
            }
            return definitions;
        }

        @Override
        public @NotNull String getName() {
            // Ugly, but harmless. This name will not be used in any schema-level context.
            return getLocalSubjectItemName().getLocalPart();
        }
    }

    static class Modern
            extends ShadowAssociationClassSimulationDefinition {

        @NotNull private final QName associationClassName;

        private Modern(
                @NotNull QName associationClassName,
                @NotNull ItemName localSubjectItemName,
                @NotNull AttributeBinding primaryAttributeBinding,
                @Nullable AttributeBinding secondaryAttributeBinding,
                @Nullable QName primaryBindingMatchingRuleLegacy,
                @NotNull ResourceObjectAssociationDirectionType direction,
                boolean requiresExplicitReferentialIntegrity,
                @NotNull ResourceObjectDefinition referentialObjectDefinition,
                @NotNull ResourceAttributeDefinition<?> subjectSidePrimaryBindingAttributeDefinition,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> subjects,
                @NotNull Collection<SimulatedAssociationClassParticipantDefinition> objects) {
            super(localSubjectItemName, primaryAttributeBinding, secondaryAttributeBinding, primaryBindingMatchingRuleLegacy,
                    direction, requiresExplicitReferentialIntegrity, referentialObjectDefinition,
                    subjectSidePrimaryBindingAttributeDefinition, subjects, objects);
            this.associationClassName = QNameUtil.enforceNamespace(associationClassName, NS_RI);
        }

        static Modern parse(
                @NotNull SimulatedAssociationClassConfigItem simulationCI,
                @Nullable ShadowAssociationTypeDefinitionConfigItem definitionCI,
                @NotNull ResourceSchemaImpl schemaBeingParsed) throws ConfigurationException {

            var primaryBinding = simulationCI.getPrimaryAttributeBinding();
            var subjectSidePrimaryBindingAttrName = primaryBinding.subjectSide();

            // TODO check the compatibility of subject/object definitions!

            var subjects = getSubjectDefinitions(simulationCI, schemaBeingParsed, definitionCI);
            var objects = getObjectDefinitions(simulationCI, schemaBeingParsed, definitionCI);

            // The "referential" association subject-side definition. It must contain the definitions of all relevant
            // subject-side binding attributes (primary/secondary). Hence, it should contain the subject's auxiliary object class
            // definition, if present.
            var referentialSubjectDefinition = subjects.iterator().next().getObjectDefinition();
            var referentialObjectDefinition = objects.iterator().next().getObjectDefinition();

            return new Modern(
                    simulationCI.getName(),
                    simulationCI.getItemName(),
                    primaryBinding,
                    simulationCI.getSecondaryAttributeBinding(),
                    null,
                    simulationCI.getDirection(),
                    simulationCI.requiresExplicitReferentialIntegrity(),
                    referentialObjectDefinition,
                    simulationCI.configNonNull(
                            referentialSubjectDefinition.findAttributeDefinition(subjectSidePrimaryBindingAttrName),
                            "No definition for subject-side primary binding attribute %s in %s",
                            subjectSidePrimaryBindingAttrName, DESC),
                    subjects,
                    objects);
        }

        private static Collection<SimulatedAssociationClassParticipantDefinition> getSubjectDefinitions(
                @NotNull SimulatedAssociationClassConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema,
                @Nullable ShadowAssociationTypeDefinitionConfigItem definitionCI) throws ConfigurationException {
            var delineationCIs = simulationCI.getSubject().getDelineations();
            if (delineationCIs.isEmpty()) {
                // This means we must use the information from association type.
                return definitionsFromAssociationType(
                        simulationCI, definitionCI, resourceSchema, true, simulationCI.getItemName());
            } else {
                return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema, simulationCI.getItemName());
            }
        }

        private static Collection<SimulatedAssociationClassParticipantDefinition> getObjectDefinitions(
                @NotNull SimulatedAssociationClassConfigItem simulationCI,
                @NotNull ResourceSchemaImpl resourceSchema,
                @Nullable ShadowAssociationTypeDefinitionConfigItem definitionCI) throws ConfigurationException {
            var delineationCIs = simulationCI.getObject().getDelineations();
            if (delineationCIs.isEmpty()) {
                // This means we must use the information from association type.
                return definitionsFromAssociationType(simulationCI, definitionCI, resourceSchema, false, null);
            } else {
                return definitionsFromSimulationConfiguration(delineationCIs, resourceSchema, null);
            }
        }

        private static @NotNull List<SimulatedAssociationClassParticipantDefinition> definitionsFromAssociationType(
                @NotNull SimulatedAssociationClassConfigItem simulationCI,
                @Nullable ShadowAssociationTypeDefinitionConfigItem definitionCI,
                @NotNull ResourceSchemaImpl resourceSchema,
                boolean subject,
                @Nullable ItemName associationItemName) throws ConfigurationException {
            var what = subject ? "subject" : "object";
            simulationCI.configCheck(definitionCI != null,
                    "No %s delineations and no association type definition; in %s", what, DESC);
            var typeIdentifiers = subject ? definitionCI.getSubjectTypeIdentifiers() : definitionCI.getObjectTypeIdentifiers();
            simulationCI.configCheck(!typeIdentifiers.isEmpty(),
                    "No %s delineations and none provided by the association type definition; in %s", what, DESC);
            return resolveTypeDefinitions(resourceSchema, typeIdentifiers, definitionCI).stream()
                    .map(def ->
                            SimulatedAssociationClassParticipantDefinition.fromObjectTypeDefinition(def, null, associationItemName))
                    .toList();
        }

        private @NotNull static List<SimulatedAssociationClassParticipantDefinition> definitionsFromSimulationConfiguration(
                List<SimulatedAssociationClassParticipantDelineationConfigItem> delineationCIs,
                @NotNull ResourceSchemaImpl resourceSchema,
                @Nullable ItemName associationItemName) throws ConfigurationException {
            List<SimulatedAssociationClassParticipantDefinition> definitions = new ArrayList<>();
            for (var delineationCI : delineationCIs) {
                QName objectClassName = delineationCI.getObjectClassName();
                definitions.add(
                        new SimulatedAssociationClassParticipantDefinition(
                                objectClassName,
                                delineationCI.getBaseContext(),
                                delineationCI.getSearchHierarchyScope(),
                                delineationCI.configNonNull(
                                        resourceSchema.findDefinitionForObjectClass(objectClassName),
                                        "No definition for object class %s found in %s", objectClassName, DESC),
                                delineationCI.getAuxiliaryObjectClassName(),
                                associationItemName));
            }
            return definitions;
        }

        private static Collection<ResourceObjectTypeDefinition> resolveTypeDefinitions(
                ResourceSchemaImpl resourceSchema,
                Collection<? extends ResourceObjectTypeIdentification> typeIdentifiers,
                ShadowAssociationTypeDefinitionConfigItem origin) throws ConfigurationException {
            Collection<ResourceObjectTypeDefinition> definitions = new ArrayList<>();
            for (var typeIdentifier : typeIdentifiers) {
                definitions.add(
                        origin.configNonNull(
                                resourceSchema.getObjectTypeDefinition(typeIdentifier),
                                "No object type definition found for %s used in %s", typeIdentifier, DESC));
            }
            return definitions;
        }

        @Override
        public @NotNull String getName() {
            return associationClassName.getLocalPart();
        }
    }
}
