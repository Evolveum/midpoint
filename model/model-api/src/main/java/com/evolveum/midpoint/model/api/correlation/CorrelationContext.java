/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * The context of the correlation and correlator state update operations.
 * (Both work on an object being synchronized. The use in the latter case is experimental, though.)
 *
 * Created by _the caller_ of {@link Correlator#correlate(CorrelationContext, OperationResult)} method, but then updated
 * by the method implementation(s) themselves.
 *
 * Not to be confused with {@link CorrelatorContext} which describes the context of the whole {@link Correlator} lifespan.
 */
public abstract class CorrelationContext implements DebugDumpable, Cloneable {

    /**
     * Focus that was created using pre-mappings.
     * May be empty (but not null) e.g. if there are no such mappings.
     */
    @NotNull final Containerable preFocus;

    /**
     * TEMPORARY
     *
     * Candidates from which we want to select the [candidate] owner/owners.
     * If null, standard repository query is executed.
     */
    @Nullable final Collection<? extends Containerable> candidatePool;

    /**
     * System configuration to use during the correlation.
     */
    @Nullable private final SystemConfigurationType systemConfiguration;

    /**
     * Task in which the correlation takes place.
     */
    @NotNull private final Task task;

    /**
     * Information about the current state of the correlator used.
     * Usually provided by upstream (parent) correlator.
     */
    private AbstractCorrelatorStateType correlatorState;

    public CorrelationContext(
            @NotNull Containerable preFocus,
            @Nullable Collection<? extends Containerable> candidatePool,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull Task task) {
        this.preFocus = preFocus;
        this.candidatePool = candidatePool;
        this.systemConfiguration = systemConfiguration;
        this.task = task;
    }

    public @NotNull FocusType getPreFocus() {
        if (preFocus instanceof FocusType focus) {
            return focus;
        } else {
            // TEMPORARY (until everything will be migrated to Containerable)
            throw new UnsupportedOperationException("Use of non-FocusType objects is not supported here: " + preFocus);
        }
    }

    // TEMPORARY (until everything will be migrated to Containerable)
    public @NotNull Containerable getPreFocusContainerable() {
        return preFocus;
    }

    // TEMPORARY (until everything will be migrated to Containerable)
    public @NotNull Class<? extends ObjectType> getFocusType() {
        return getPreFocus().getClass();
    }

    // TEMPORARY (until everything will be migrated to Containerable)
    public @NotNull Class<? extends Containerable> getFocusContainerableType() {
        return preFocus.getClass();
    }

    public @Nullable Collection<? extends Containerable> getCandidatePool() {
        return candidatePool;
    }

    /** Returns the archetype for focus objects that the candidate(s) must possess. Null means "no restrictions".  */
    public abstract @Nullable String getArchetypeOid();

    /**
     * Returns candidate owners provided by previous correlator(s), if any.
     *
     * Background: If more child correlators are defined to be used, they will run separately (at least under
     * the default implementation of the composite correlator), one after another. The original implementation executed
     * each of the correlators independently, so that (typically) each of them issued its own query over all population
     * of focus objects. The results were then combined by the composite correlator.
     *
     * However, there might be situations where subsequent correlators should just _refine_ the results returned
     * by previous one(s). For that, we want to retain the relevant candidate owner(s) OID(s) in the context, and
     * use that to limit search within those correlators.
     *
     * Empty set means "no previous candidates available", i.e. no restrictions will be applied.
     *
     * LIMITED USE. Currently used only "identification recovery" feature - for {@link Focus} context and `items` correlator.
     */
    public abstract @NotNull Set<String> getCandidateOids();

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    public AbstractCorrelatorStateType getCorrelatorState() {
        return correlatorState;
    }

    public void setCorrelatorState(AbstractCorrelatorStateType correlatorState) {
        this.correlatorState = correlatorState;
    }

    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "focusType", getFocusContainerableType(), indent + 1);
        debugDumpSpecific(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "systemConfiguration", String.valueOf(systemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "correlatorState", correlatorState, indent + 1);
        return sb.toString();
    }

    void debugDumpSpecific(StringBuilder sb, int indent) {
        // Nothing to do here. To be overridden by subclasses.
    }

    public @NotNull abstract Shadow asShadowCtx();

    /**
     * A simple shallow clone. Use with care.
     */
    @Override
    public CorrelationContext clone() {
        try {
            return (CorrelationContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SystemException(e);
        }
    }

    /** Returns the object (e.g. shadow or focus or its part) that is being correlated. Currently for logging purposes. */
    @NotNull public abstract Containerable getPrimaryCorrelatedObject();

    /** Context for correlating a shadow to a set of matching focuses. */
    public static class Shadow extends CorrelationContext {

        /**
         * Shadowed resource object to be correlated.
         */
        @NotNull private final ShadowType resourceObject;

        /**
         * Resource on which the correlated shadow resides.
         */
        @NotNull private final ResourceType resource;

        /**
         * Usually resource object type definition (~ schemaHandling section).
         */
        @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

        public Shadow(
                @NotNull ShadowType resourceObject,
                @NotNull ResourceType resource,
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull Containerable preFocus,
                @Nullable Collection<? extends Containerable> candidatePool,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task) {
            super(preFocus, candidatePool, systemConfiguration, task);
            this.resourceObject = resourceObject;
            this.resource = resource;
            this.resourceObjectDefinition = resourceObjectDefinition;
        }

        public @NotNull ShadowType getResourceObject() {
            return resourceObject;
        }

        public @NotNull ResourceType getResource() {
            return resource;
        }

        public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
            return resourceObjectDefinition;
        }

        public @Nullable String getArchetypeOid() {
            // Note that the archetype OID is not supported for legacy synchronization definition.
            // Therefore we may safely access it in the following way:
            return resourceObjectDefinition.getFocusSpecification().getArchetypeOid();
        }

        @Override
        public @NotNull Set<String> getCandidateOids() {
            return Set.of();
        }

        @Override
        public @NotNull Shadow asShadowCtx() {
            return this;
        }

        @Override
        public @NotNull Containerable getPrimaryCorrelatedObject() {
            return resourceObject;
        }

        @Override
        public String toString() {
            return "CorrelationContext.Shadow("
                    + getFocusContainerableType().getSimpleName() + ", "
                    + resourceObjectDefinition.getHumanReadableName() + "@" + resource
                    + ')';
        }

        @Override
        void debugDumpSpecific(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "resource", String.valueOf(resource), indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", String.valueOf(resourceObjectDefinition), indent + 1);
        }
    }

    /** Context for correlating an association value to a set of matching focus assignments. */
    public static class AssociationValue extends CorrelationContext {

        @NotNull private final ShadowAssociationValue associationValue;

        public AssociationValue(
                @NotNull ShadowAssociationValue associationValue,
                @NotNull Containerable preFocus,
                @Nullable Collection<? extends Containerable> candidatePool,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task) {
            super(preFocus, candidatePool, systemConfiguration, task);
            this.associationValue = associationValue;
        }

        public @Nullable String getArchetypeOid() {
            return null;
        }

        @Override
        public @NotNull Set<String> getCandidateOids() {
            return Set.of();
        }

        @Override
        public @NotNull Shadow asShadowCtx() {
            throw new IllegalStateException("Association value context cannot be used as shadow context");
        }

        @Override
        public @NotNull Containerable getPrimaryCorrelatedObject() {
            return associationValue.asContainerable();
        }

        @Override
        public String toString() {
            return "CorrelationContext.AssociationValue()";
        }

        @Override
        void debugDumpSpecific(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "association value", associationValue, indent + 1);
        }
    }

    /**
     * Context for correlating a focus to a set of matching focuses.
     *
     * TODO finish this class
     */
    public static class Focus extends CorrelationContext {

        private final String archetypeOid;
        private final Set<String> candidateOids;

        public Focus(
                @NotNull Containerable preFocus,
                @Nullable String archetypeOid,
                @NotNull Set<String> candidateOids,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task) {
            super(preFocus, null, systemConfiguration, task);
            this.archetypeOid = archetypeOid;
            this.candidateOids = candidateOids;
        }

        @Override
        public @NotNull Shadow asShadowCtx() {
            throw new IllegalStateException("Focus context cannot be used as shadow context");
        }

        @Override
        public @Nullable String getArchetypeOid() {
            return archetypeOid;
        }

        @Override
        public @NotNull Set<String> getCandidateOids() {
            return candidateOids;
        }

        @Override
        public @NotNull Containerable getPrimaryCorrelatedObject() {
            return preFocus;
        }

        @Override
        public String toString() {
            return "CorrelationContext.Focus("
                    + getFocusContainerableType().getSimpleName() + ", "
                    + preFocus
                    + ')';
        }
    }
}
