/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    @NotNull final FocusType preFocus;

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
            @NotNull FocusType preFocus,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull Task task) {
        this.preFocus = preFocus;
        this.systemConfiguration = systemConfiguration;
        this.task = task;
    }

    public @NotNull FocusType getPreFocus() {
        return preFocus;
    }

    public @NotNull Class<? extends ObjectType> getFocusType() {
        return preFocus.getClass();
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
        DebugUtil.debugDumpWithLabelLn(sb, "focusType", getFocusType(), indent + 1);
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

    /** Returns the object (e.g. shadow or focus) that is being correlated. Currently for logging purposes. */
    @NotNull public abstract ObjectType getPrimaryCorrelatedObject();

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
                @NotNull FocusType preFocus,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task) {
            super(preFocus, systemConfiguration, task);
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
            // Note that the archetype OID can be specified only on the object type. It is not supported
            // for legacy synchronization definition. Therefore we may safely access it in the following way:
            ResourceObjectTypeDefinition typeDefinition = resourceObjectDefinition.getTypeDefinition();
            return typeDefinition != null ? typeDefinition.getArchetypeOid() : null;
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
        public @NotNull ObjectType getPrimaryCorrelatedObject() {
            return resourceObject;
        }

        @Override
        public String toString() {
            return "CorrelationContext.Shadow("
                    + getFocusType().getSimpleName() + ", "
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

    /**
     * Context for correlating a focus to a set of matching focuses.
     *
     * TODO finish this class
     */
    public static class Focus extends CorrelationContext {

        private final String archetypeOid;
        private final Set<String> candidateOids;

        public Focus(
                @NotNull FocusType preFocus,
                @Nullable String archetypeOid,
                @NotNull Set<String> candidateOids,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task) {
            super(preFocus, systemConfiguration, task);
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
        public @NotNull ObjectType getPrimaryCorrelatedObject() {
            return preFocus;
        }

        @Override
        public String toString() {
            return "CorrelationContext.Focus("
                    + getFocusType().getSimpleName() + ", "
                    + preFocus
                    + ')';
        }
    }
}
