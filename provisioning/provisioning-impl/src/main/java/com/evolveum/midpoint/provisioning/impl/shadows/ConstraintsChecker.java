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

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
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

/**
 * @author semancik
 * @author mederly
 */
public class ConstraintsChecker {

    private static final Trace LOGGER = TraceManager.getTrace(ConstraintsChecker.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private static final ConcurrentHashMap<Thread, Cache> CACHE_INSTANCES = new ConcurrentHashMap<>();

    private ProvisioningContext provisioningContext;
    private PrismContext prismContext;
    private CacheConfigurationManager cacheConfigurationManager;
    private ShadowsFacade shadowsFacade;
    private final StringBuilder messageBuilder = new StringBuilder();
    private PrismObject<ShadowType> shadowObject;
    private PrismObject<ShadowType> shadowObjectOld;
    private String shadowOid;
    private ConstraintViolationConfirmer constraintViolationConfirmer;
    private boolean useCache = true;
    private ConstraintsCheckingStrategyType strategy;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public void setProvisioningContext(ProvisioningContext provisioningContext) {
        this.provisioningContext = provisioningContext;
    }

    public void setCacheConfigurationManager(CacheConfigurationManager cacheConfigurationManager) {
        this.cacheConfigurationManager = cacheConfigurationManager;
    }

    public ShadowsFacade getShadowCache() {
        return shadowsFacade;
    }

    public void setShadowsFacade(ShadowsFacade shadowsFacade) {
        this.shadowsFacade = shadowsFacade;
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

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public void setStrategy(ConstraintsCheckingStrategyType strategy) {
        this.strategy = strategy;
    }

    private ConstraintsCheckingResult constraintsCheckingResult;

    public ConstraintsCheckingResult check(Task task, OperationResult parentResult) throws SchemaException,
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

            RefinedObjectClassDefinition objectClassDefinition = provisioningContext.getObjectClassDefinition();
            Collection<? extends ResourceAttributeDefinition> uniqueAttributeDefs = getUniqueAttributesDefinitions();
            LOGGER.trace("Checking uniqueness of attributes: {}", uniqueAttributeDefs);
            for (ResourceAttributeDefinition attrDef : uniqueAttributeDefs) {
                PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getItemName());
                LOGGER.trace("Attempt to check uniqueness of {} (def {})", attr, attrDef);
                if (attr == null) {
                    continue;
                }
                constraintsCheckingResult.getCheckedAttributes().add(attr.getElementName());
                boolean unique = checkAttributeUniqueness(attr, objectClassDefinition, provisioningContext.getResource(),
                        shadowOid, task, result);
                if (!unique) {
                    LOGGER.debug("Attribute {} conflicts with existing object (in {})", attr,
                            provisioningContext.getShadowCoordinates());
                    constraintsCheckingResult.getConflictingAttributes().add(attr.getElementName());
                    constraintsCheckingResult.setSatisfiesConstraints(false);
                }
            }
            constraintsCheckingResult.setMessages(messageBuilder.toString());
            return constraintsCheckingResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    public Collection<? extends RefinedAttributeDefinition<?>> getUniqueAttributesDefinitions()
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        RefinedObjectClassDefinition objectClassDefinition = provisioningContext.getObjectClassDefinition();
        //noinspection unchecked
        return MiscUtil.unionExtends(objectClassDefinition.getPrimaryIdentifiers(),
                objectClassDefinition.getSecondaryIdentifiers());
    }

    private boolean checkAttributeUniqueness(PrismProperty identifier, RefinedObjectClassDefinition accountDefinition,
                                             ResourceType resourceType, String oid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        List<PrismPropertyValue<?>> identifierValues = identifier.getValues();
        if (identifierValues.isEmpty()) {
            throw new SchemaException("Empty identifier "+identifier+" while checking uniqueness of "+oid+" ("+resourceType+")");
        }

        //TODO: set matching rule instead of null
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .itemWithDef(identifier.getDefinition(), ShadowType.F_ATTRIBUTES, identifier.getDefinition().getItemName())
                        .eq(PrismValueCollectionsUtil.cloneCollection(identifierValues))
                .and().item(ShadowType.F_OBJECT_CLASS).eq(accountDefinition.getObjectClassDefinition().getTypeName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
                .and().block()
                    .item(ShadowType.F_DEAD).eq(false)
                    .or().item(ShadowType.F_DEAD).isNull()
                .endBlock()
                .build();
        boolean unique = checkUniqueness(oid, identifier, query, task, result);
        return unique;
    }

    private boolean checkUniqueness(String oid, PrismProperty identifier, ObjectQuery query, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        RefinedObjectClassDefinition objectClassDefinition = provisioningContext.getObjectClassDefinition();
        ResourceType resourceType = provisioningContext.getResource();
        if (useCache && Cache.isOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getItemName(), identifier.getValues(), cacheConfigurationManager)) {
            return true;
        }

        // Note that we should not call repository service directly here. The query values need to be normalized according to
        // attribute matching rules.
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        List<PrismObject<ShadowType>> foundObjects = shadowsFacade.searchObjects(query, options, task, result);
        LOGGER.trace("Uniqueness check of {} resulted in {} results:\n{}\nquery:\n{}",
                identifier, foundObjects.size(), foundObjects, query.debugDumpLazily(1));
        if (foundObjects.isEmpty()) {
            if (useCache) {
                Cache.setOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getItemName(), identifier.getValues());
            }
            return true;
        }
        if (foundObjects.size() > 1) {
            LOGGER.error("Found {} objects with attribute {}:\n{}", foundObjects.size() ,identifier.toHumanReadableString(), foundObjects);
            if (LOGGER.isDebugEnabled()) {
                for (PrismObject<ShadowType> foundObject: foundObjects) {
                    LOGGER.debug("Conflicting object:\n{}", foundObject.debugDump());
                }
            }
            message("Found more than one object with attribute "+identifier.toHumanReadableString());
            return false;
        }
        LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
        boolean match = foundObjects.get(0).getOid().equals(oid);
        if (!match) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found conflicting existing object with attribute " + identifier.toHumanReadableString() + ":\n"
                        + foundObjects.get(0).debugDump());
            }
            message("Found conflicting existing object with attribute " + identifier.toHumanReadableString() + ": " + foundObjects.get(0));
            match = !constraintViolationConfirmer.confirmViolation(foundObjects.get(0));
            constraintsCheckingResult.setConflictingShadow(foundObjects.get(0));
            // we do not cache "OK" here because the violation confirmer could depend on attributes/items that are not under our observations
        } else {
            if (useCache) {
                Cache.setOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getItemName(), identifier.getValues());
            }
            return true;
        }
        return match;
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

    public static <T extends ShadowType> void onShadowAddOperation(T shadow) {
        Cache cache = Cache.getCache();
        if (cache != null) {
            log("Clearing cache on shadow add operation", false);
            cache.conflictFreeSituations.clear();            // TODO fix this brute-force approach
        }
    }

    public static void onShadowModifyOperation(Collection<? extends ItemDelta> deltas) {
        // here we must be very cautious; we do not know which attributes are naming ones!
        // so in case of any attribute change, let's clear the cache
        // (actually, currently only naming attributes are stored in repo)
        Cache cache = Cache.getCache();
        if (cache == null) {
            return;
        }
        ItemPath attributesPath = ShadowType.F_ATTRIBUTES;
        for (ItemDelta itemDelta : deltas) {
            if (attributesPath.isSubPathOrEquivalent(itemDelta.getParentPath())) {
                log("Clearing cache on shadow attribute modify operation", false);
                cache.conflictFreeSituations.clear();
                return;
            }
        }
    }

    public boolean canSkipChecking()
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (shadowObjectOld == null) {
            return false;
        } else if (shadowObject == null) {
            LOGGER.trace("Skipping uniqueness checking because objectNew is null");
            return true;
        } else if (strategy == null || !Boolean.TRUE.equals(strategy.isSkipWhenNoChange())) {
            LOGGER.trace("Uniqueness checking will not be skipped because 'skipWhenNoChange' is not set");
            return false;
        } else {
            for (RefinedAttributeDefinition<?> definition : getUniqueAttributesDefinitions()) {
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
        Set attributeValues;

        public Situation(String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName, Set attributeValues) {
            this.resourceOid = resourceOid;
            this.knownShadowOid = knownShadowOid;
            this.objectClassName = objectClassName;
            this.attributeName = attributeName;
            this.attributeValues = attributeValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Situation situation = (Situation) o;

            if (!attributeName.equals(situation.attributeName)) return false;
            if (attributeValues != null ? !attributeValues.equals(situation.attributeValues) : situation.attributeValues != null)
                return false;
            if (knownShadowOid != null ? !knownShadowOid.equals(situation.knownShadowOid) : situation.knownShadowOid != null)
                return false;
            if (objectClassName != null ? !objectClassName.equals(situation.objectClassName) : situation.objectClassName != null)
                return false;
            if (!resourceOid.equals(situation.resourceOid)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = resourceOid.hashCode();
            result = 31 * result + (knownShadowOid != null ? knownShadowOid.hashCode() : 0);
            result = 31 * result + (objectClassName != null ? objectClassName.hashCode() : 0);
            result = 31 * result + attributeName.hashCode();
            result = 31 * result + (attributeValues != null ? attributeValues.hashCode() : 0);
            return result;
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
                List attributeValues, CacheConfigurationManager cacheConfigurationManager) {
            return isOk(new Situation(resourceOid, knownShadowOid, objectClassName, attributeName, getRealValuesSet(attributeValues)),
                    cacheConfigurationManager);
        }

        private static boolean isOk(Situation situation,
                CacheConfigurationManager cacheConfigurationManager) {
            if (situation.attributeValues == null) {        // special case - problem - TODO implement better
                return false;
            }

            CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;
            Cache cache = getCache();
            CacheConfiguration configuration = cache != null ? cache.getConfiguration() :
                    cacheConfigurationManager.getConfiguration(CacheType.LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE);
            CacheConfiguration.CacheObjectTypeConfiguration objectTypeConfiguration = configuration != null ?
                    configuration.getForObjectType(ShadowType.class) : null;
            CacheConfiguration.StatisticsLevel statisticsLevel = CacheConfiguration.getStatisticsLevel(objectTypeConfiguration, configuration);
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

        private static void setOk(String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName, List attributeValues) {
            setOk(new Situation(resourceOid, knownShadowOid, objectClassName, attributeName, getRealValuesSet(attributeValues)));
        }

        private static Set getRealValuesSet(List attributeValues) {
            Set retval = new HashSet();
            for (Object attributeValue : attributeValues) {
                if (attributeValue == null) {
                    // can be skipped
                } else if (attributeValue instanceof PrismPropertyValue) {
                    retval.add(((PrismPropertyValue) attributeValue).getValue());
                } else {
                    LOGGER.warn("Unsupported attribute value: {}", attributeValue);
                    return null;        // a problem!
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
