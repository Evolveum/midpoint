/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.archetypes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.merger.template.ObjectTemplateMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
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
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Component that can efficiently determine archetypes for objects.
 * It is backed by caches, therefore this is supposed to be a low-overhead service that can be
 * used in many places.
 *
 * As a secondary responsibility, this class handles the resolution of object templates.
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
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;

    /** Indexed by archetype OID. */
    private final Map<String, ArchetypePolicyType> archetypePolicyCache = new ConcurrentHashMap<>();

    /** Contains IMMUTABLE expanded object templates. */
    private final ObjectTemplateCache objectTemplateCache = new ObjectTemplateCache();

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
    public ArchetypeType determineStructuralArchetype(@Nullable ObjectType object, OperationResult result)
            throws SchemaException {
        return ArchetypeTypeUtil.getStructuralArchetype(
                determineArchetypes(object, result));
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
        return determineArchetypePolicy(
                determineArchetypes(object, result),
                object,
                result);
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
                .toList();
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

    /** A convenience variant of {@link #getPolicyForArchetype(ArchetypeType, OperationResult)}. */
    public @Nullable ArchetypePolicyType getPolicyForArchetype(@NotNull String archetypeOid, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        return getPolicyForArchetype(
                getArchetype(archetypeOid, result),
                result);
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
        // FIXME cache also empty (null) policies, but obviously not as "null" values
        ArchetypePolicyType mergedPolicy = computePolicyForArchetype(archetype, result);
        if (mergedPolicy != null) {
            archetypePolicyCache.put(archetype.getOid(), mergedPolicy);
        }
        return mergedPolicy;
    }

    /**
     * Computes policy merged from this archetype and its super-archetypes.
     *
     * TODO we should return an empty policy even if it does not exist, in order to cache it
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

    /** Determines default object policy configuration for the specified object type */
    public <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            @NotNull Class<O> objectClass, OperationResult result) throws SchemaException, ConfigurationException {
        SystemConfigurationType systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
        if (systemConfiguration == null) {
            return null;
        }
        return determineObjectPolicyConfiguration(
                objectClass,
                null,
                systemConfiguration);
    }

    /** Determines legacy object policy configuration (from subtypes) */
    public ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
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

    private static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
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
                throw new ConfigurationException(
                        "Unknown type " + typeQName + " in default object policy definition in system configuration");
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

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // This seems to be harsh (and probably is), but a policy can really depend on a mix of objects. So we have to play
        // it safely and invalidate eagerly. Hopefully objects of these types do not change often.
        if (type == null || INVALIDATION_RELATED_CLASSES.contains(type)) {
            archetypePolicyCache.clear();
            objectTemplateCache.clear();
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

    /**
     * Returns the "expanded" object template, i.e. the one with "include" instructions resolved.
     *
     * Note that the handling of dangling references is graceful just like in the case of archetypes,
     * see the note in the class-level javadoc. However, if the `oid` parameter cannot be resolved,
     * the respective exception is thrown.
     *
     * @return Immutable expanded template
     */
    public @NotNull ObjectTemplateType getExpandedObjectTemplate(
            @NotNull String oid, @NotNull TaskExecutionMode executionMode, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        ObjectTemplateType cached = objectTemplateCache.get(oid, executionMode);
        if (cached != null) {
            return cached;
        }

        ObjectTemplateType main = cacheRepositoryService
                .getObject(ObjectTemplateType.class, oid, null, result)
                .asObjectable();
        expandObjectTemplate(main, new ResolutionChain(oid), executionMode, result);
        main.freeze();
        objectTemplateCache.put(main, executionMode);
        return main;
    }

    /**
     * Expands the object template by merging the (recursively) expanded included templates into it.
     *
     * Because of the "multiple inheritance" - i.e., multiple potential includeRef values - any particular included template
     * may be fetched multiple times. This should present little harm, as everything should be resolved from repo cache,
     * and expanded versions are cached as well.
     *
     * The `resolutionChain` contains OIDs on the "resolution path" from the main template to the currently-expanded one.
     * It is used to check for cycles as well as to describe the path in case of problems.
     */
    private void expandObjectTemplate(
            ObjectTemplateType template, ResolutionChain resolutionChain, TaskExecutionMode executionMode, OperationResult result)
            throws ConfigurationException, SchemaException {
        if (!SimulationUtil.isVisible(template, executionMode)) {
            LOGGER.trace("Not expanding template {} as it is not visible for the current task", template);
        } else {
            List<ObjectReferenceType> includeRefList = template.getIncludeRef();
            LOGGER.trace("Starting expansion of {}: {} include(s); chain = {}", template, includeRefList.size(), resolutionChain);
            for (ObjectReferenceType includeRef : includeRefList) {
                String includedOid =
                        MiscUtil.configNonNull(includeRef.getOid(), () -> "OID-less includeRef is not supported in " + template);
                ObjectTemplateType included =
                        getExpandedObjectTemplateInternal(includedOid, resolutionChain, executionMode, result);
                if (included != null) {
                    if (!SimulationUtil.isVisible(included, executionMode)) {
                        LOGGER.trace("Not including template {} as it is not visible for the current task", template);
                    } else {
                        new ObjectTemplateMergeOperation(template, included)
                                .execute();
                    }
                }
            }
            LOGGER.trace("Expansion of {} done; chain = {}", template, resolutionChain);
        }
    }

    /** @return Mutable expanded template - to allow potential modifications during merging (is this really needed?) */
    private @Nullable ObjectTemplateType getExpandedObjectTemplateInternal(
            String oid, ResolutionChain resolutionChain, TaskExecutionMode executionMode, OperationResult result)
            throws SchemaException, ConfigurationException {

        ObjectTemplateType cached = objectTemplateCache.get(oid, executionMode);
        if (cached != null) {
            return cached.clone();
        }

        resolutionChain.addAndCheckForCycles(oid);
        try {
            ObjectTemplateType template;
            try {
                template = cacheRepositoryService
                        .getObject(ObjectTemplateType.class, oid, null, result)
                        .asObjectable();
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Included object template {} was not found; the resolution chain is: {}", oid, resolutionChain);
                return null;
            }
            expandObjectTemplate(template, resolutionChain, executionMode, result);
            objectTemplateCache.put(template, executionMode);
            return template;
        } finally {
            resolutionChain.removeLast(oid);
        }
    }

    private static class ResolutionChain {

        private final Deque<String> oids = new ArrayDeque<>();

        ResolutionChain(@NotNull String initial) {
            oids.addLast(initial);
        }

        void addAndCheckForCycles(@NotNull String oid) throws ConfigurationException {
            configCheck(
                    !oids.contains(oid), "A cycle in object template references: %s", this);
            oids.addLast(oid);
        }

        void removeLast(@NotNull String oid) {
            String last = oids.removeLast();
            stateCheck(
                    oid.equals(last),
                    "Unexpected last element of resolution chain: %s (expected %s); remainder = %s",
                    last, oid, this);
        }

        @Override
        public String toString() {
            return String.join(" -> ", oids);
        }
    }

    /** Stores cached object templates. Currently only for production-configuration execution modes. */
    private static class ObjectTemplateCache {

        /** Indexed by OID. Contains immutable objects. */
        private final Map<String, ObjectTemplateType> objects = new ConcurrentHashMap<>();

        void clear() {
            objects.clear();
        }

        /** Returns immutable object. */
        ObjectTemplateType get(@NotNull String oid, @NotNull TaskExecutionMode executionMode) {
            if (executionMode.isProductionConfiguration()) {
                return objects.get(oid);
            } else {
                return null;
            }
        }

        /** Does not modify the object being added - creates a clone, if needed. */
        void put(@NotNull ObjectTemplateType template, @NotNull TaskExecutionMode executionMode) {
            if (executionMode.isProductionConfiguration()) {
                objects.put(
                        Objects.requireNonNull(template.getOid()),
                        CloneUtil.toImmutable(template));
            }
        }
    }

    public boolean isOfArchetype(AssignmentHolderType assignmentHolderType, String archetypeOid, OperationResult result) throws SchemaException, ConfigurationException {
        List<ArchetypeType> archetypes = determineArchetypes(assignmentHolderType, result);

        Optional<ArchetypeType> resultedArchetype = archetypes.stream()
                .filter(archetype -> archetype.getOid().equals(archetypeOid))
                .findFirst();
        if (resultedArchetype.isPresent()) {
            return true;
        }

        return checkSuperArchetypes(archetypes, archetypeOid, result);
    }

    public boolean isSubArchetypeOrArchetype(String archetypeOid, String parentArchetype, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (archetypeOid.equals(parentArchetype)) {
            return true;
        }
        ArchetypeType archetypeType = getArchetype(archetypeOid, result);
        ObjectReferenceType superArchetype = archetypeType.getSuperArchetypeRef();
        if (superArchetype == null) {
            return false;
        }
        return isSubArchetypeOrArchetype(superArchetype.getOid(), parentArchetype, result);
    }

    private boolean checkSuperArchetypes(List<ArchetypeType> archetypes, String archetypeOid, OperationResult result) throws SchemaException, ConfigurationException {
        List<ArchetypeType> superArchetypes = new ArrayList<>();
        for (ArchetypeType archetype : archetypes) {
            ArchetypeType superArchetype = getSuperArchetype(archetype, result);
            if (superArchetype == null) {
                continue;
            }
            if (superArchetype.getOid().equals(archetypeOid)) {
                return true;
            }
            superArchetypes.add(superArchetype);
        }
        if (superArchetypes.isEmpty()) {
            return false;
        }
        return checkSuperArchetypes(superArchetypes, archetypeOid, result);
    }
}
