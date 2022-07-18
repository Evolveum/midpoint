/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.archetypes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Component that can efficiently determine archetypes for objects.
 * It is backed by caches, therefore this is supposed to be a low-overhead service that can be
 * used in many places.
 *
 * [NOTE]
 * ====
 * When resolving archetype references (i.e. obtaining archetype objects from references in object assignments and
 * `archetypeRef` values, as well as when resolving super-archetypes), we currently handle dangling references
 * (non-existing objects) by ignoring them. We just log the exception, and keep the {@link OperationResultStatus#FATAL_ERROR}
 * in the result tree - where the lower-level code put it. (It may or may not be available to the ultimate caller; depending
 * on the overall operation result processing.)
 * ====
 *
 * @author Radovan Semancik
 */
@Component
public class ArchetypeManager implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeManager.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(ArchetypeManager.class.getName() + ".content");

    /**
     * Cache invalidation is invoked when an object of any of these classes is modified.
     */
    private static final Collection<Class<?>> INVALIDATION_RELATED_CLASSES = Arrays.asList(
            ArchetypeType.class,
            SystemConfigurationType.class,
            ObjectTemplateType.class
    );

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private ArchetypeDeterminer archetypeDeterminer;
    @Autowired private ArchetypePolicyMerger archetypePolicyMerger;

    private final Map<String, ArchetypePolicyType> archetypePolicyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    /**
     * Gets an archetype by OID. Assumes that caching of {@link ArchetypeType} objects is enabled by global repository cache.
     * (By default, it is - see `default-caching-profile.xml` in `infra/schema` resources.)
     */
    public @NotNull ArchetypeType getArchetype(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return systemObjectCache
                .getArchetype(oid, result)
                .asObjectable();
    }

    /**
     * Resolves archetype OIDs to full objects.
     *
     * See the class-level note about resolving dangling references.
     */
    public List<ArchetypeType> resolveArchetypeOids(Collection<String> oids, Object context, OperationResult result)
            throws SchemaException {
        List<ArchetypeType> archetypes = new ArrayList<>();
        for (String oid : oids) {
            try {
                archetypes.add(
                        getArchetype(oid, result));
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Archetype {} for {} cannot be found", oid, context);
            }
        }
        return archetypes;
    }

    /**
     * Determines all archetype OIDs (structural + auxiliary) for a given (static) object.
     *
     * @see #determineArchetypeOids(ObjectType, ObjectType)
     */
    public @NotNull Set<String> determineArchetypeOids(@Nullable ObjectType object) {
        return archetypeDeterminer.determineArchetypeOids(object);
    }

    /**
     * Determines all archetype OIDs in the "dynamic" case where the object is changed during clockwork processing.
     *
     * @see #determineArchetypeOids(ObjectType)
     */
    public <O extends ObjectType> @NotNull Set<String> determineArchetypeOids(O before, O after) {
        return archetypeDeterminer.determineArchetypeOids(before, after);
    }

    /**
     * Determines all archetypes for a "static" object.
     *
     * See the class-level note about resolving dangling references.
     */
    public @NotNull List<ArchetypeType> determineArchetypes(@Nullable ObjectType object, OperationResult result)
            throws SchemaException {
        return resolveArchetypeOids(
                archetypeDeterminer.determineArchetypeOids(object),
                object,
                result);
    }

    /**
     * Determines the structural archetype for a "static" object.
     *
     * (See the class-level note about resolving dangling references.)
     */
    public ArchetypeType determineStructuralArchetype(@Nullable AssignmentHolderType assignmentHolder, OperationResult result)
            throws SchemaException {
        return ArchetypeTypeUtil.getStructuralArchetype(
                determineArchetypes(assignmentHolder, result));
    }

    /**
     * Determines the archetype policy for an object.
     *
     * (See the class-level note about resolving dangling archetype references.)
     *
     * @see #determineArchetypePolicy(Collection, ObjectType, OperationResult)
     */
    public ArchetypePolicyType determineArchetypePolicy(@Nullable ObjectType object, OperationResult result)
            throws SchemaException, ConfigurationException {
        Set<String> archetypeOids = archetypeDeterminer.determineArchetypeOids(object);
        List<ArchetypeType> archetypes = resolveArchetypeOids(archetypeOids, object, result);
        return determineArchetypePolicy(archetypes, object, result);
    }

    /**
     * A convenience variant of {@link #determineArchetypePolicy(ObjectType, OperationResult)}.
     */
    public ArchetypePolicyType determineArchetypePolicy(
            @Nullable PrismObject<? extends ObjectType> object, OperationResult result)
            throws SchemaException, ConfigurationException {
        return determineArchetypePolicy(asObjectable(object), result);
    }

    /**
     * Determines "complex" archetype policy; takes auxiliary archetypes, super archetypes,
     * and even legacy (subtype) configuration into account.
     */
    public ArchetypePolicyType determineArchetypePolicy(
            Collection<ArchetypeType> allArchetypes,
            ObjectType object,
            OperationResult result)
            throws SchemaException, ConfigurationException {
        return archetypePolicyMerger.merge(
                determineArchetypePolicyFromArchetypes(allArchetypes, result),
                determineObjectPolicyConfiguration(object, result));
    }

    /**
     * Determines "pure" archetype policy - just from archetypes, ignoring subtype configuration.
     */
    private ArchetypePolicyType determineArchetypePolicyFromArchetypes(
            Collection<ArchetypeType> allArchetypes, OperationResult result)
            throws SchemaException, ConfigurationException {

        if (allArchetypes.isEmpty()) {
            return null;
        }

        ArchetypeType structuralArchetype = ArchetypeTypeUtil.getStructuralArchetype(allArchetypes);
        List<ArchetypeType> auxiliaryArchetypes = allArchetypes.stream()
                .filter(ArchetypeTypeUtil::isAuxiliary)
                .collect(Collectors.toList());
        if (structuralArchetype == null && !auxiliaryArchetypes.isEmpty()) {
            throw new SchemaException("Auxiliary archetype cannot be assigned without structural archetype");
        }

        // TODO collision detection

        ArchetypePolicyType mergedPolicy = new ArchetypePolicyType();

        for (ArchetypeType auxiliaryArchetype : auxiliaryArchetypes) {
            mergedPolicy = archetypePolicyMerger.mergeArchetypePolicies(
                    mergedPolicy,
                    getPolicyForArchetype(auxiliaryArchetype, result));
        }

        assert structuralArchetype != null;
        mergedPolicy = archetypePolicyMerger.mergeArchetypePolicies(
                mergedPolicy,
                getPolicyForArchetype(structuralArchetype, result));

        return mergedPolicy;
    }

    /**
     * Returns policy collected from this archetype and its super-archetypes. Uses the policy cache.
     */
    public ArchetypePolicyType getPolicyForArchetype(ArchetypeType archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (archetype == null) {
            return null;
        }

        if (archetype.getOid() == null) {
            // The policy is certainly not cached. But we may try to compute it.
            // TODO is this code path ever used?
            return computePolicyForArchetype(archetype, result);
        }

        ArchetypePolicyType cachedPolicy = archetypePolicyCache.get(archetype.getOid());
        if (cachedPolicy != null) {
            return cachedPolicy;
        }
        ArchetypePolicyType mergedPolicy = computePolicyForArchetype(archetype, result);
        if (mergedPolicy != null) {
            archetypePolicyCache.put(archetype.getOid(), mergedPolicy);
        }
        return mergedPolicy;
    }

    /**
     * Computes policy merged from this archetype and its super-archetypes.
     */
    private ArchetypePolicyType computePolicyForArchetype(ArchetypeType archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        ArchetypePolicyType currentPolicy = archetype.getArchetypePolicy();
        ArchetypeType superArchetype = getSuperArchetype(archetype, result);
        if (superArchetype == null) {
            return currentPolicy;
        } else {
            ArchetypePolicyType superPolicy = getPolicyForArchetype(superArchetype, result);
            return archetypePolicyMerger.mergeArchetypePolicies(superPolicy, currentPolicy);
        }
    }

    private @Nullable ArchetypeType getSuperArchetype(ArchetypeType archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        ObjectReferenceType superArchetypeRef = archetype.getSuperArchetypeRef();
        if (superArchetypeRef == null) {
            return null;
        }
        String oid = MiscUtil.configNonNull(
                superArchetypeRef.getOid(),
                () -> "Dynamic (non-OID) super-archetype references are not supported; in " + archetype);
        try {
            return systemObjectCache
                    .getArchetype(oid, result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Super archetype {} (of {}) couldn't be found", oid, archetype);
            return null;
        }
    }

    /** Determines legacy object policy configuration (from subtypes) */
    private ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            ObjectType object, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (object == null) {
            return null;
        }
        SystemConfigurationType systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
        if (systemConfiguration == null) {
            return null;
        }
        return determineObjectPolicyConfiguration(object, systemConfiguration);
    }

    private static ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            ObjectType object, SystemConfigurationType systemConfiguration) throws ConfigurationException {
        return determineObjectPolicyConfiguration(
                object.getClass(),
                FocusTypeUtil.determineSubTypes(object),
                systemConfiguration);
    }

    public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            Class<O> objectClass,
            List<String> objectSubtypes,
            SystemConfigurationType systemConfiguration) throws ConfigurationException {
        ObjectPolicyConfigurationType applicablePolicyConfigurationType = null;
        for (ObjectPolicyConfigurationType aPolicyConfiguration: systemConfiguration.getDefaultObjectPolicyConfiguration()) {
            QName typeQName = aPolicyConfiguration.getType();
            if (typeQName == null) {
                continue; // TODO implement correctly (using 'applicable policies' perhaps)
            }
            ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
            if (objectType == null) {
                throw new ConfigurationException("Unknown type "+typeQName+" in default object policy definition in system configuration");
            }
            if (objectType.getClassDefinition() == objectClass) {
                String aSubType = aPolicyConfiguration.getSubtype();
                if (aSubType == null) {
                    if (applicablePolicyConfigurationType == null) {
                        applicablePolicyConfigurationType = aPolicyConfiguration;
                    }
                } else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
                    applicablePolicyConfigurationType = aPolicyConfiguration;
                }
            }
        }
        return applicablePolicyConfigurationType;
    }

    // TODO take object's archetype into account
    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(
            PrismObject<O> object, PrismObject<SystemConfigurationType> systemConfiguration) throws ConfigurationException {
        if (systemConfiguration == null) {
            return null;
        }
        return determineLifecycleModel(object, systemConfiguration.asObjectable());
    }

    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(
            PrismObject<O> object, SystemConfigurationType systemConfiguration) throws ConfigurationException {
        ObjectPolicyConfigurationType objectPolicyConfiguration =
                determineObjectPolicyConfiguration(asObjectable(object), systemConfiguration);
        if (objectPolicyConfiguration == null) {
            return null;
        }
        return objectPolicyConfiguration.getLifecycleStateModel();
    }

    public <O extends ObjectType> ExpressionProfile determineExpressionProfile(PrismObject<O> object, OperationResult result)
            throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicy = determineArchetypePolicy(object, result);
        if (archetypePolicy == null) {
            return null;
        }
        String expressionProfileId = archetypePolicy.getExpressionProfile();
        return systemObjectCache.getExpressionProfile(expressionProfileId, result);
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // This seems to be harsh (and probably is), but a policy can really depend on a mix of objects. So we have to play
        // it safely and invalidate eagerly. Hopefully objects of these types do not change often.
        if (type == null || INVALIDATION_RELATED_CLASSES.contains(type)) {
            archetypePolicyCache.clear();
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType()
                .name(ArchetypeManager.class.getName())
                .size(archetypePolicyCache.size()));
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            archetypePolicyCache.forEach((k, v) -> LOGGER_CONTENT.info("Cached archetype policy: {}: {}", k, v));
        }
    }
}
