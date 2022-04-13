/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static java.util.Objects.requireNonNullElse;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.UNKNOWN;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronization policy bean ({@link ObjectSynchronizationType}) along with the corresponding {@link ResourceObjectDefinition}.
 *
 * Note that the resource object definition is usually {@link ResourceObjectTypeDefinition}, but in some exceptional cases
 * it may be {@link ResourceObjectClassDefinition} instead. (TODO: really it may be?)
 */
public class ResourceObjectTypeSynchronizationPolicy {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeSynchronizationPolicy.class);

    @NotNull private final ShadowKindType kind;
    @NotNull private final ObjectSynchronizationType synchronizationBean;
    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

    private ResourceObjectTypeSynchronizationPolicy(
            @NotNull ShadowKindType kind,
            @NotNull ObjectSynchronizationType synchronizationBean,
            @NotNull ResourceObjectDefinition resourceObjectDefinition) {
        this.kind = kind;
        this.synchronizationBean = synchronizationBean;
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    /**
     * Creates {@link ResourceObjectTypeSynchronizationPolicy} for a policy embedded in a resource object type definition
     * (i.e. in schema handling section).
     */
    public static @NotNull ResourceObjectTypeSynchronizationPolicy forEmbedded(
            @NotNull ResourceObjectTypeDefinition typeDef, @NotNull ObjectSynchronizationType syncDef) {
        // TODO is this fixing necessary?
        ObjectSynchronizationType clone = syncDef.clone();
        clone.setKind(typeDef.getKind());
        clone.setIntent(typeDef.getIntent()); // "Parsed" intent, e.g. converted from null to something sensible
        if (clone.getObjectClass().isEmpty()) {
            clone.getObjectClass().add(
                    typeDef.getObjectClassName());
        }
        return new ResourceObjectTypeSynchronizationPolicy(typeDef.getKind(), clone, typeDef);
    }

    /**
     * Creates {@link ResourceObjectTypeSynchronizationPolicy} for a synchronization policy present in "synchronization"
     * section of the resource definition. We try to find appropriate object/class definition in the resource schema.
     *
     * Returns null if no such definition can be found.
     */
    public static @Nullable ResourceObjectTypeSynchronizationPolicy forStandalone(
            @NotNull ObjectSynchronizationType synchronizationBean, @NotNull ResourceSchema schema) {
        ShadowKindType kind = requireNonNullElse(synchronizationBean.getKind(), ACCOUNT);
        String intent = synchronizationBean.getIntent();

        ResourceObjectDefinition policyObjectDefinition;
        if (StringUtils.isEmpty(intent)) { // Note: intent shouldn't be the empty string!
            QName objectClassName = synchronizationBean.getObjectClass().size() == 1 ?
                    synchronizationBean.getObjectClass().get(0) : null;
            policyObjectDefinition = schema.findObjectDefinition(kind, null, objectClassName); // TODO check this
        } else {
            policyObjectDefinition = schema.findObjectDefinition(kind, intent);
        }

        if (policyObjectDefinition != null) {
            return new ResourceObjectTypeSynchronizationPolicy(kind, synchronizationBean, policyObjectDefinition);
        } else {
            return null;
        }
    }

    /**
     * Creates {@link ResourceObjectTypeSynchronizationPolicy} by looking for type definition and synchronization
     * for given kind and intent in resource schema.
     */
    public static @Nullable ResourceObjectTypeSynchronizationPolicy forKindAndIntent(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {

        Preconditions.checkArgument(ShadowUtil.isKnown(kind), "kind is not known: %s", kind);
        Preconditions.checkArgument(ShadowUtil.isKnown(intent), "intent is not known: %s", intent);

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        ResourceObjectTypeSynchronizationPolicy embeddedPolicy = getEmbeddedPolicyIfPresent(kind, intent, schema);
        if (embeddedPolicy != null) {
            return embeddedPolicy;
        } else {
            return getStandalonePolicyIfPresent(kind, intent, resource, schema);
        }
    }

    /** Temporary implementation. */
    public static @Nullable ResourceObjectTypeSynchronizationPolicy forTypeDefinition(
            @NotNull ResourceObjectTypeDefinition definition, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        return forKindAndIntent(
                definition.getKind(),
                definition.getIntent(),
                resource);
    }

    private static @Nullable ResourceObjectTypeSynchronizationPolicy getEmbeddedPolicyIfPresent(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceSchema schema) {
        ResourceObjectDefinition definition = schema.findObjectDefinition(kind, intent);
        if (definition instanceof ResourceObjectTypeDefinition) {
            ResourceObjectTypeDefinition typeDefinition = (ResourceObjectTypeDefinition) definition;
            ObjectSynchronizationType embedded = typeDefinition.getDefinitionBean().getSynchronization();
            if (embedded != null) {
                return forEmbedded(typeDefinition, embedded);
            }
        }
        return null;
    }

    private static @Nullable ResourceObjectTypeSynchronizationPolicy getStandalonePolicyIfPresent(
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @NotNull ResourceType resource,
            @NotNull ResourceSchema schema) {
        if (resource.getSynchronization() == null) {
            return null;
        }
        // We don't directly compare bean.intent, because the binding of sync <-> schema handling may be implicit
        // using object class name (todo - really? or just I think so?)
        for (ObjectSynchronizationType synchronizationBean : resource.getSynchronization().getObjectSynchronization()) {
            ResourceObjectTypeSynchronizationPolicy standalone = forStandalone(synchronizationBean, schema);
            if (standalone != null
                    && kind == standalone.getKind()
                    && intent.equals(standalone.getIntent())) {
                return standalone;
            }
        }
        return null;
    }

    /**
     * Looks up the policy corresponding to the object type definition *bean* retrieved from schema handling.
     */
    public static @Nullable ResourceObjectTypeSynchronizationPolicy forDefinitionBean(
            @NotNull ResourceObjectTypeDefinitionType typeDefBean, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (schema == null) {
            return null;
        }
        ResourceObjectDefinition objectDefinition =
                schema.findObjectDefinition(typeDefBean.getKind(), typeDefBean.getIntent(), typeDefBean.getObjectClass());
        if (objectDefinition instanceof ResourceObjectTypeDefinition) {
            return forDefinition((ResourceObjectTypeDefinition) objectDefinition, resource);
        } else {
            // shouldn't occur
            return null;
        }
    }

    private static @Nullable ResourceObjectTypeSynchronizationPolicy forDefinition(
            @NotNull ResourceObjectTypeDefinition typeDefinition, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        if (typeDefinition.getDefinitionBean().getSynchronization() != null) {
            return forEmbedded(typeDefinition, typeDefinition.getDefinitionBean().getSynchronization());
        } else {
            // We know kind + intent, so we can look up directly using these
            return forKindAndIntent(typeDefinition.getKind(), typeDefinition.getIntent(), resource);
        }
    }

    public @NotNull ShadowKindType getKind() {
        return kind;
    }

    /**
     * The returned intent is null only if:
     *
     * 1. standalone synchronization bean is used,
     * 2. the intent is not specified in the bean,
     * 3. no default object type can be found.
     */
    public @Nullable String getIntent() {
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return ((ResourceObjectTypeDefinition) resourceObjectDefinition).getIntent();
        } else {
            return null;
        }
    }

    public @NotNull ObjectSynchronizationType getSynchronizationBean() {
        return synchronizationBean;
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    /**
     * Returns true if the policy is applicable to given synchronization discriminator (sorter result):
     * compares its kind and intent.
     */
    public boolean isApplicableToSynchronizationDiscriminator(@NotNull ObjectSynchronizationDiscriminatorType discriminator) {
        ShadowKindType kind = discriminator.getKind();
        String intent = discriminator.getIntent();
        if (kind == null || intent == null) {
            throw new IllegalArgumentException(
                    "Object synchronization discriminator must have both kind and intent specified. "
                            + "Current values are: kind=" + kind + ", intent=" + intent);
        }
        return isApplicableTo(null, kind, intent, false);
    }

    /**
     * Compares the policy to given shadow - it may or may not be classified (i.e. having kind/intent specified).
     */
    public boolean isApplicableToShadow(@NotNull ShadowType shadow) throws SchemaException {
        return isApplicableTo(
                MiscUtil.requireNonNull(
                        shadow.getObjectClass(),
                        () -> "No object class in " + shadow),
                shadow.getKind(), // nullable if shadow is not classified yet
                shadow.getIntent(), // nullable if shadow is not classified yet
                false);
    }

    /**
     * Checks if the synchronization policy matches given "parameters" (object class, kind, intent).
     */
    public boolean isApplicableTo(QName objectClass, ShadowKindType kind, String intent, boolean strictIntent) {
        if (objectClassDefinedAndNotMatching(objectClass, synchronizationBean.getObjectClass())) {
            LOGGER.trace("Object class does not match the one defined in {}", this);
            return false;
        }

        if (objectClassDefinedAndNotMatching(objectClass, List.of(resourceObjectDefinition.getTypeName()))) {
            LOGGER.trace("Object class does not match the one defined in type definition in {}", this);
            return false;
        }

        // kind
        LOGGER.trace("Comparing kinds, policy kind: {}, current kind: {}", getKind(), kind);
        if (kind != null && kind != UNKNOWN && !getKind().equals(kind)) {
            LOGGER.trace("Kinds don't match for {}", this);
            return false;
        }

        // intent
        LOGGER.trace("Comparing intents, policy intent: {}, current intent: {} (strict={})", getIntent(), intent, strictIntent);
        if (!strictIntent) {
            if (intent != null
                    && !SchemaConstants.INTENT_UNKNOWN.equals(intent)
                    && !MiscSchemaUtil.equalsIntent(intent, getIntent())) {
                LOGGER.trace("Intents don't match for {}", this);
                return false;
            }
        } else {
            if (!MiscSchemaUtil.equalsIntent(intent, getIntent())) {
                LOGGER.trace("Intents don't match for {}", this);
                return false;
            }
        }

        return true;
    }

    private boolean objectClassDefinedAndNotMatching(@Nullable QName objectClass, @NotNull List<QName> policyObjectClasses) {
        return objectClass != null &&
                !policyObjectClasses.isEmpty() &&
                !QNameUtil.matchAny(objectClass, policyObjectClasses);
    }


    public ResourceObjectTypeDefinition getResourceTypeDefinitionRequired() {
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) resourceObjectDefinition;
        } else {
            throw new IllegalStateException("No resource object type definition present: " + resourceObjectDefinition);
        }
    }

    public @NotNull CompositeCorrelatorType getCorrelatorsCloned() {
        if (getConfiguredCorrelators() != null) {
            return getConfiguredCorrelators().clone();
        } else if (synchronizationBean.getCorrelation().isEmpty()) {
            return new CompositeCorrelatorType()
                    .beginNone().end();
        } else {
            CompositeCorrelatorType correlators =
                    new CompositeCorrelatorType()
                            .beginFilter()
                            .confirmation(CloneUtil.clone(getSynchronizationBean().getConfirmation()))
                            .end();
            correlators.getFilter().get(0).getOwnerFilter().addAll(
                    CloneUtil.cloneCollectionMembers(getSynchronizationBean().getCorrelation()));
            return correlators;
        }
    }

    private CompositeCorrelatorType getConfiguredCorrelators() {
        if (synchronizationBean.getCorrelationDefinition() != null) {
            return synchronizationBean.getCorrelationDefinition().getCorrelators();
        } else {
            return null;
        }
    }

    /** Returns the focus type this synchronization policy points to. */
    public @NotNull QName getFocusTypeName() {
        return Objects.requireNonNullElse(
                synchronizationBean.getFocusType(),
                UserType.COMPLEX_TYPE);
    }

    /** Returns the focus class this synchronization policy points to. */
    public @NotNull Class<? extends FocusType> getFocusClass() {
        return PrismContext.get().getSchemaRegistry()
                .determineClassForTypeRequired(
                        getFocusTypeName());
    }

    @Override
    public String toString() {
        return "ResourceObjectTypeSynchronizationPolicy{" +
                "kind=" + kind +
                ", synchronizationBean=" + synchronizationBean +
                ", resourceObjectDefinition=" + resourceObjectDefinition +
                '}';
    }
}
