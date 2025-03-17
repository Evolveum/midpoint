/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.schema.cache.AbstractThreadLocalCache;
import com.evolveum.midpoint.schema.cache.CacheConfiguration;
import com.evolveum.midpoint.schema.cache.CachePerformanceCollector;
import com.evolveum.midpoint.schema.cache.CacheUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import static java.util.Objects.requireNonNull;

/**
 * @author semancik
 *
 */
public class FocusConstraintsChecker<AH extends AssignmentHolderType> {

    private static ConcurrentHashMap<Thread, Cache> cacheInstances = new ConcurrentHashMap<>();

    private static final Trace LOGGER = TraceManager.getTrace(FocusConstraintsChecker.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private LensContext<AH> context;
    private PrismContext prismContext;
    private RepositoryService repositoryService;
    private CacheConfigurationManager cacheConfigurationManager;
    private boolean satisfiesConstraints;
    private final StringBuilder messageBuilder = new StringBuilder();
    private PrismObject<AH> conflictingObject;

    private SingleLocalizableMessage localizableMessage;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public LensContext<AH> getContext() {
        return context;
    }

    public void setContext(LensContext<AH> context) {
        this.context = context;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public void setCacheConfigurationManager(CacheConfigurationManager cacheConfigurationManager) {
        this.cacheConfigurationManager = cacheConfigurationManager;
    }

    public boolean isSatisfiesConstraints() {
        return satisfiesConstraints;
    }

    public String getMessages() {
        return messageBuilder.toString();
    }

    public SingleLocalizableMessage getLocalizableMessage() {
        return localizableMessage;
    }

    public PrismObject<AH> getConflictingObject() {
        return conflictingObject;
    }
    public boolean check(PrismObject<AH> objectNew, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.subresult(FocusConstraintsChecker.class.getName() + ".check")
                .setMinor()
                .build();
        try {
            if (objectNew == null) {
                // This must be delete
                LOGGER.trace("No new object. Therefore it satisfies constraints");
                satisfiesConstraints = true;
            } else {
                // Hardcode to name ... for now
                PolyStringType name = objectNew.asObjectable().getName();
                if (Cache.isOk(name, cacheConfigurationManager)) {
                    satisfiesConstraints = true;
                } else {
                    satisfiesConstraints = checkPropertyUniqueness(objectNew, ObjectType.F_NAME, context, result);
                    if (satisfiesConstraints) {
                        Cache.setOk(name);
                    }
                }
            }
            return satisfiesConstraints;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <T> boolean checkPropertyUniqueness(PrismObject<AH> objectNew, ItemPath propPath, LensContext<AH> context, OperationResult result) throws SchemaException {

        PrismProperty<T> property = objectNew.findProperty(propPath);
        if (property == null || property.isEmpty()) {
            throw new SchemaException("No property "+propPath+" in new object "+objectNew+", cannot check uniqueness");
        }
        String oid = objectNew.getOid();

        ObjectQuery query;
        Class<AH> objectClass = requireNonNull(objectNew.getCompileTimeClass());
        if (property.getDefinition() != null && QNameUtil.match(PolyStringType.COMPLEX_TYPE, property.getDefinition().getTypeName())) {
            List<PrismPropertyValue<T>> clonedValues = (List<PrismPropertyValue<T>>) PrismValueCollectionsUtil.cloneCollection(property.getValues());
            query = prismContext.queryFor(objectClass)
                        .item(property.getPath())
                        .eq(clonedValues)
                        .matchingOrig()
                    .or()
                        .item(property.getPath())
                        .eq(clonedValues)
                        .matchingNorm()
                    .build();
        } else {
            query = prismContext.queryFor(objectClass)
                .itemAs(property)
                .build();
        }

        List<PrismObject<AH>> foundObjects = repositoryService.searchObjects(objectClass, query, readOnly(), result);
        LOGGER.trace("Uniqueness check of {}, property {} resulted in {} results, using query:\n{}",
                objectNew, propPath, foundObjects.size(), query.debugDumpLazily());
        if (foundObjects.isEmpty()) {
            return true;
        }
        if (foundObjects.size() > 1) {
            LOGGER.trace("Found more than one object with property {} = {}: {}", propPath, property, foundObjects.size());
            message("Found more than one object with property "+propPath+" = " + property);
            return false;
        }

        LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
        boolean match = foundObjects.get(0).getOid().equals(oid);
        if (!match) {
            LOGGER.trace("Found conflicting existing object with property {} = {}:\n{}",
                    propPath, property, foundObjects.get(0).debugDumpLazily(1));
            message("Found conflicting existing object with property "+propPath+" = " + property + ": "
                    + foundObjects.get(0));

            // move to message()
            localizableMessage =  new SingleLocalizableMessage("FocusConstraintsChecker.object.already.exists",
                    new Object[]{
                        new SingleLocalizableMessage("ObjectType." + objectNew.getCompileTimeClass().getSimpleName()),
                        propPath,
                        property.getRealValues()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))
                    });

            conflictingObject = foundObjects.get(0);
        }

        return match;
    }

    private void message(String message) {
        if (!messageBuilder.isEmpty()) {
            messageBuilder.append(", ");
        }
        messageBuilder.append(message);
    }

    public static void enterCache(CacheConfiguration configuration) {
        Cache.enter(cacheInstances, Cache.class, configuration, LOGGER);
    }

    public static void exitCache() {
        Cache.exit(cacheInstances, LOGGER);
    }

    public static <T extends ObjectType> void clearCacheFor(PolyStringType name) {
        Cache.remove(name);
    }

    public static void clearCacheForValues(Collection<? extends PrismValue> values) {
        if (values == null) {
            return;
        }
        for (PrismValue value : values) {
            if (value instanceof PrismPropertyValue) {
                Object real = ((PrismPropertyValue) value).getValue();
                if (real instanceof PolyStringType) {
                    clearCacheFor((PolyStringType) real);
                }
            }
        }
    }

    public static void clearCacheForDelta(Collection<? extends ItemDelta> modifications) {
        if (modifications == null) {
            return;
        }
        for (ItemDelta itemDelta : modifications) {
            if (ObjectType.F_NAME.equivalent(itemDelta.getPath())) {
                clearCacheForValues(itemDelta.getValuesToAdd());            // these may present a conflict
                clearCacheForValues(itemDelta.getValuesToReplace());        // so do these
            }
        }
    }

    public static class Cache extends AbstractThreadLocalCache {

        private static final Trace LOGGER_CONTENT = TraceManager.getTrace(Cache.class.getName() + ".content");

        private final Set<String> conflictFreeNames = ConcurrentHashMap.newKeySet();

        static boolean isOk(PolyStringType name, CacheConfigurationManager cacheConfigurationManager) {
            if (name == null) {
                log("Null name", false);
                return false;            // strange case
            }

            CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;
            Cache cache = getCache();
            var configuration = cache != null ? cache.getConfiguration() :
                    cacheConfigurationManager.getConfiguration(CacheType.LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE);
            var objectTypeConfiguration = configuration != null ?
                    configuration.getForTypeIgnoringObjectClass(FocusType.class) : null;
            CacheConfiguration.StatisticsLevel statisticsLevel = CacheConfiguration.getStatisticsLevel(objectTypeConfiguration, configuration);
            boolean traceMiss = CacheConfiguration.getTraceMiss(objectTypeConfiguration, configuration);

            if (cache == null) {
                log("Cache NULL for {}", false, name);
                collector.registerNotAvailable(Cache.class, FocusType.class, statisticsLevel);
                return false;
            }

            if (cache.conflictFreeNames.contains(name.getOrig())) {
                log("Cache HIT for {}", false, name);
                cache.registerHit();
                collector.registerHit(Cache.class, FocusType.class, statisticsLevel);
                return true;
            } else {
                log("Cache MISS for {}", traceMiss, name);
                cache.registerMiss();
                collector.registerMiss(Cache.class, FocusType.class, statisticsLevel);
                return false;
            }
        }

        public static void setOk(PolyStringType name) {
            Cache cache = getCache();
            if (name != null && cache != null) {
                cache.conflictFreeNames.add(name.getOrig());
            }
        }

        private static Cache getCache() {
            return cacheInstances.get(Thread.currentThread());
        }

        public static void remove(PolyStringType name) {
            Cache cache = getCache();
            if (name != null && cache != null) {
                log("Cache REMOVE for {}", false, name);
                cache.conflictFreeNames.remove(name.getOrig());
            }
        }

        @Override
        public String description() {
            return "conflict-free names: " + conflictFreeNames;
        }

        @Override
        protected int getSize() {
            return conflictFreeNames.size();
        }

        @Override
        protected void dumpContent(String threadName) {
            if (LOGGER_CONTENT.isInfoEnabled()) {
                conflictFreeNames.forEach(name -> LOGGER_CONTENT.info("Cached conflict-free name [{}]: {}", threadName, name));
            }
        }

        private static void log(String message, boolean info, Object... params) {
            CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
        }
    }
}
