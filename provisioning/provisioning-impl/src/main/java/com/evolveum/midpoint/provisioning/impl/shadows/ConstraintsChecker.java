/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.caching.CacheUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstraintsCheckingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

/**
 * @author semancik
 */
public class ConstraintsChecker {

    private static final Trace LOGGER = TraceManager.getTrace(ConstraintsChecker.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private static final ConcurrentHashMap<Thread, Cache> CACHE_INSTANCES = new ConcurrentHashMap<>();

    private ProvisioningContext provisioningContext;
    private final StringBuilder messageBuilder = new StringBuilder();
    private PrismObject<ShadowType> shadowObject;
    private PrismObject<ShadowType> shadowObjectOld;
    private String shadowOid;
    private ConstraintViolationConfirmer constraintViolationConfirmer;
    private boolean useCache = true;
    private ConstraintsCheckingStrategyType strategy;
    private final ShadowsLocalBeans shadowsLocalBeans = ShadowsLocalBeans.get();

    public void setProvisioningContext(ProvisioningContext provisioningContext) {
        this.provisioningContext = provisioningContext;
    }

    public void setShadowObject(PrismObject<ShadowType> shadowObject) {
        this.shadowObject = shadowObject;
    }

    public void setShadowObjectOld(
            PrismObject<ShadowType> shadowObjectOld) {
        this.shadowObjectOld = shadowObjectOld;
    }

    public void setShadowOid(String shadowOid) {
        this.shadowOid = shadowOid;
    }

    public void setConstraintViolationConfirmer(ConstraintViolationConfirmer constraintViolationConfirmer) {
        this.constraintViolationConfirmer = constraintViolationConfirmer;
    }

    void setDoNotUseCache() {
        this.useCache = false;
    }

    public void setStrategy(ConstraintsCheckingStrategyType strategy) {
        this.strategy = strategy;
    }

    private ConstraintsCheckingResult constraintsCheckingResult;

    public ConstraintsCheckingResult check(OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(ConstraintsChecker.class.getName() + ".check")
                .setMinor()
                .build();
        try {
            constraintsCheckingResult = new ConstraintsCheckingResult();
            constraintsCheckingResult.setSatisfiesConstraints(true);

            PrismContainer<?> attributesContainer = shadowObject.findContainer(ShadowType.F_ATTRIBUTES);
            if (attributesContainer == null) {
                // No attributes no constraint violations
                LOGGER.trace("Current shadow does not contain attributes, skipping checking uniqueness.");
                return constraintsCheckingResult;
            }

            Collection<? extends ResourceAttributeDefinition<?>> uniqueAttributeDefs = getUniqueAttributesDefinitions();
            LOGGER.trace("Checking uniqueness of attributes: {}", uniqueAttributeDefs);
            for (ResourceAttributeDefinition<?> attrDef : uniqueAttributeDefs) {
                PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getItemName());
                LOGGER.trace("Attempt to check uniqueness of {} (def {})", attr, attrDef);
                if (attr == null) {
                    continue;
                }
                constraintsCheckingResult.getCheckedAttributes().add(attr.getElementName());
                boolean unique = checkAttributeUniqueness(attr, result);
                if (!unique) {
                    LOGGER.debug("Attribute {} conflicts with existing object (in {})", attr, provisioningContext);
                    constraintsCheckingResult.getConflictingAttributes().add(attr.getElementName());
                    constraintsCheckingResult.setSatisfiesConstraints(false);
                }
            }
            constraintsCheckingResult.setMessages(messageBuilder.toString());
            return constraintsCheckingResult;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    // What attributes should be used for uniqueness checking? Currently: all identifiers.
    private @NotNull Collection<? extends ResourceAttributeDefinition<?>> getUniqueAttributesDefinitions() {
        return provisioningContext.getObjectDefinitionRequired().getAllIdentifiers();
    }

    private boolean checkAttributeUniqueness(PrismProperty<?> identifier, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {

        ResourceType resource = provisioningContext.getResource();
        ResourceObjectDefinition definition = provisioningContext.getObjectDefinitionRequired();

        List<? extends PrismPropertyValue<?>> identifierValues = identifier.getValues();
        schemaCheck(!identifierValues.isEmpty(),
            "Empty identifier %s while checking uniqueness of %s (%s)", identifier, shadowOid, resource);

        // Note that we do not set matching rule here. We assume that values in repo and here are already normalized.
        ObjectQuery query = PrismContext.get().queryFor(ShadowType.class)
                .itemWithDef(identifier.getDefinition(), ShadowType.F_ATTRIBUTES, identifier.getDefinition().getItemName())
                        .eq(PrismValueCollectionsUtil.cloneCollection(identifierValues))
                .and().item(ShadowType.F_OBJECT_CLASS).eq(definition.getObjectClassName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and().block()
                    .item(ShadowType.F_DEAD).eq(false)
                    .or().item(ShadowType.F_DEAD).isNull()
                .endBlock()
                .build();
        return checkUniquenessByQuery(identifier, query, result);
    }

    private boolean checkUniquenessByQuery(
            PrismProperty<?> identifier,
            ObjectQuery query,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        ResourceObjectDefinition resourceObjectDefinition = provisioningContext.getObjectDefinitionRequired();

        ResourceType resourceType = provisioningContext.getResource();
        if (useCache && Cache.isOk(
                resourceType.getOid(),
                shadowOid,
                resourceObjectDefinition.getTypeName(),
                identifier.getDefinition().getItemName(),
                identifier.getValues(),
                shadowsLocalBeans.cacheConfigurationManager)) {
            return true;
        }

        // Note that we should not call repository service directly here. The query values need to be normalized according to
        // attribute matching rules.
        List<PrismObject<ShadowType>> matchingObjects =
                shadowsLocalBeans.shadowsFacade.searchObjects(
                        query,
                        GetOperationOptions.createNoFetchCollection(),
                        provisioningContext.getTask(),
                        result);
        LOGGER.trace("Uniqueness check of {} resulted in {} results:\n{}\nquery:\n{}",
                identifier, matchingObjects.size(), matchingObjects, query.debugDumpLazily(1));
        if (matchingObjects.isEmpty()) {
            if (useCache) {
                Cache.setOk(
                        resourceType.getOid(),
                        shadowOid,
                        resourceObjectDefinition.getTypeName(),
                        identifier.getDefinition().getItemName(),
                        identifier.getValues());
            }
            return true;
        }
        if (matchingObjects.size() > 1) {
            LOGGER.error("Found {} objects with attribute {}:\n{}",
                    matchingObjects.size(), identifier.toHumanReadableString(), matchingObjects);
            if (LOGGER.isDebugEnabled()) {
                for (PrismObject<ShadowType> foundObject: matchingObjects) {
                    LOGGER.debug("Conflicting object:\n{}", foundObject.debugDump());
                }
            }
            message("Found more than one object with attribute "+identifier.toHumanReadableString());
            return false;
        }
        LOGGER.trace("Comparing {} and {}", matchingObjects.get(0).getOid(), shadowOid);
        boolean unique = matchingObjects.get(0).getOid().equals(shadowOid);
        if (!unique) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found conflicting existing object with attribute " + identifier.toHumanReadableString() + ":\n"
                        + matchingObjects.get(0).debugDump());
            }
            message("Found conflicting existing object with attribute " + identifier.toHumanReadableString()
                    + ": " + matchingObjects.get(0));
            unique = !constraintViolationConfirmer.confirmViolation(matchingObjects.get(0));
            constraintsCheckingResult.setConflictingShadow(matchingObjects.get(0));
            // We do not cache "OK" here because the violation confirmer could depend on
            // attributes/items that are not under our observations.
        } else {
            if (useCache) {
                Cache.setOk(
                        resourceType.getOid(),
                        shadowOid,
                        resourceObjectDefinition.getTypeName(),
                        identifier.getDefinition().getItemName(),
                        identifier.getValues());
            }
            return true;
        }
        return unique;
    }

    private void message(String message) {
        if (messageBuilder.length() != 0) {
            messageBuilder.append(", ");
        }
        messageBuilder.append(message);
    }

    public static void enterCache(CacheConfiguration configuration) {
        Cache.enter(CACHE_INSTANCES, Cache.class, configuration, LOGGER);
    }

    public static void exitCache() {
        Cache.exit(CACHE_INSTANCES, LOGGER);
    }

    @SuppressWarnings("unused") // Will be needed in the future
    public static <T extends ShadowType> void onShadowAddOperation(T shadow) {
        Cache cache = Cache.getCache();
        if (cache != null) {
            log("Clearing cache on shadow add operation", false);
            cache.conflictFreeSituations.clear(); // TODO fix this brute-force approach
        }
    }

    public static void onShadowModifyOperation(Collection<? extends ItemDelta<?, ?>> deltas) {
        // here we must be very cautious; we do not know which attributes are naming ones!
        // so in case of any attribute change, let's clear the cache
        // (actually, currently only naming attributes are stored in repo)
        Cache cache = Cache.getCache();
        if (cache == null) {
            return;
        }
        ItemPath attributesPath = ShadowType.F_ATTRIBUTES;
        for (ItemDelta<?, ?> itemDelta : deltas) {
            if (attributesPath.isSubPathOrEquivalent(itemDelta.getParentPath())) {
                log("Clearing cache on shadow attribute modify operation", false);
                cache.conflictFreeSituations.clear();
                return;
            }
        }
    }

    public boolean canSkipChecking() {
        if (shadowObjectOld == null) {
            return false;
        } else if (shadowObject == null) {
            LOGGER.trace("Skipping uniqueness checking because objectNew is null");
            return true;
        } else if (strategy == null || !Boolean.TRUE.equals(strategy.isSkipWhenNoChange())) {
            LOGGER.trace("Uniqueness checking will not be skipped because 'skipWhenNoChange' is not set");
            return false;
        } else {
            for (ResourceAttributeDefinition<?> definition : getUniqueAttributesDefinitions()) {
                Object oldItem = shadowObjectOld.find(ItemPath.create(ShadowType.F_ATTRIBUTES, definition.getItemName()));
                Object newItem = shadowObject.find(ItemPath.create(ShadowType.F_ATTRIBUTES, definition.getItemName()));
                if (!Objects.equals(oldItem, newItem)) {
                    LOGGER.trace("Uniqueness check will not be skipped because identifier {} values do not match: old={}, new={}",
                            definition.getItemName(), oldItem, newItem);
                    return false;
                }
            }
            LOGGER.trace("Skipping uniqueness checking because old and new values for identifiers match");
            return true;
        }
    }

    static private class Situation {
        String resourceOid;
        String knownShadowOid;
        QName objectClassName;
        QName attributeName;
        Set<?> attributeValues;

        Situation(String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName, Set<?> attributeValues) {
            this.resourceOid = resourceOid;
            this.knownShadowOid = knownShadowOid;
            this.objectClassName = objectClassName;
            this.attributeName = attributeName;
            this.attributeValues = attributeValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Situation situation = (Situation) o;
            return Objects.equals(resourceOid, situation.resourceOid)
                    && Objects.equals(knownShadowOid, situation.knownShadowOid)
                    && Objects.equals(objectClassName, situation.objectClassName)
                    && Objects.equals(attributeName, situation.attributeName)
                    && Objects.equals(attributeValues, situation.attributeValues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceOid, knownShadowOid, objectClassName, attributeName, attributeValues);
        }

        @Override
        public String toString() {
            return "Situation{" +
                    "resourceOid='" + resourceOid + '\'' +
                    ", knownShadowOid='" + knownShadowOid + '\'' +
                    ", objectClassName=" + objectClassName +
                    ", attributeName=" + attributeName +
                    ", attributeValues=" + attributeValues +
                    '}';
        }
    }

    public static class Cache extends AbstractThreadLocalCache {

        private static final Trace LOGGER_CONTENT = TraceManager.getTrace(Cache.class.getName() + ".content");

        private final Set<Situation> conflictFreeSituations = ConcurrentHashMap.newKeySet();

        private static boolean isOk(String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName,
                List<?> attributeValues, CacheConfigurationManager cacheConfigurationManager) {
            return isOk(
                    new Situation(
                            resourceOid,
                            knownShadowOid,
                            objectClassName,
                            attributeName,
                            getRealValuesSet(attributeValues)),
                    cacheConfigurationManager);
        }

        private static boolean isOk(Situation situation,
                CacheConfigurationManager cacheConfigurationManager) {
            if (situation.attributeValues == null) { // special case - problem - TODO implement better
                return false;
            }

            CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;
            Cache cache = getCache();
            CacheConfiguration configuration = cache != null ? cache.getConfiguration() :
                    cacheConfigurationManager.getConfiguration(CacheType.LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE);
            CacheConfiguration.CacheObjectTypeConfiguration objectTypeConfiguration = configuration != null ?
                    configuration.getForObjectType(ShadowType.class) : null;
            CacheConfiguration.StatisticsLevel statisticsLevel =
                    CacheConfiguration.getStatisticsLevel(objectTypeConfiguration, configuration);
            boolean traceMiss = CacheConfiguration.getTraceMiss(objectTypeConfiguration, configuration);

            if (cache == null) {
                log("Cache NULL for {}", false, situation);
                collector.registerNotAvailable(Cache.class, ShadowType.class, statisticsLevel);
                return false;
            }

            if (cache.conflictFreeSituations.contains(situation)) {
                log("Cache HIT for {}", false, situation);
                cache.registerHit();
                collector.registerHit(Cache.class, ShadowType.class, statisticsLevel);
                return true;
            } else {
                log("Cache MISS for {}", traceMiss, situation);
                cache.registerMiss();
                collector.registerMiss(Cache.class, ShadowType.class, statisticsLevel);
                return false;
            }
        }

        private static void setOk(
                String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName, List<?> attributeValues) {
            setOk(new Situation(resourceOid, knownShadowOid, objectClassName, attributeName, getRealValuesSet(attributeValues)));
        }

        private static Set<Object> getRealValuesSet(List<?> attributeValues) {
            Set<Object> retval = new HashSet<>();
            for (Object attributeValue : attributeValues) {
                if (attributeValue == null) {
                    // can be skipped
                } else if (attributeValue instanceof PrismPropertyValue) {
                    retval.add(((PrismPropertyValue<?>) attributeValue).getValue());
                } else {
                    LOGGER.warn("Unsupported attribute value: {}", attributeValue);
                    return null; // a problem!
                }
            }
            return retval;
        }

        public static void setOk(Situation situation) {
            Cache cache = getCache();
            if (cache != null) {
                cache.conflictFreeSituations.add(situation);
            }
        }

        private static Cache getCache() {
            return CACHE_INSTANCES.get(Thread.currentThread());
        }

        @Override
        public String description() {
            return "conflict-free situations: " + conflictFreeSituations;
        }

        @Override
        protected int getSize() {
            return conflictFreeSituations.size();
        }

        @Override
        protected void dumpContent(String threadName) {
            if (LOGGER_CONTENT.isInfoEnabled()) {
                conflictFreeSituations.forEach(situation ->
                        LOGGER_CONTENT.info("Cached conflict-free situation [{}]: {}", threadName, situation));
            }
        }
    }

    private static void log(String message, boolean info, Object... params) {
        CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
    }
}
