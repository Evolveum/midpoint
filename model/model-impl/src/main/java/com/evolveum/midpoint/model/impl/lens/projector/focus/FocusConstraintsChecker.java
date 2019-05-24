/*
 * Copyright (c) 2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.caching.CacheUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class FocusConstraintsChecker<AH extends AssignmentHolderType> {

	private static ThreadLocal<Cache> cacheThreadLocal = new ThreadLocal<>();

	private static final Trace LOGGER = TraceManager.getTrace(FocusConstraintsChecker.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	private LensContext<AH> context;
	private PrismContext prismContext;
	private RepositoryService repositoryService;
	private CacheConfigurationManager cacheConfigurationManager;
	private boolean satisfiesConstraints;
	private StringBuilder messageBuilder = new StringBuilder();
	private PrismObject<AH> conflictingObject;

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

	public PrismObject<AH> getConflictingObject() {
		return conflictingObject;
	}
	public void check(PrismObject<AH> objectNew, OperationResult result) throws SchemaException {

		if (objectNew == null) {
			// This must be delete
			LOGGER.trace("No new object. Therefore it satisfies constraints");
			satisfiesConstraints = true;
			return;
		}

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

	private <T> boolean checkPropertyUniqueness(PrismObject<AH> objectNew, ItemPath propPath, LensContext<AH> context, OperationResult result) throws SchemaException {

		PrismProperty<T> property = objectNew.findProperty(propPath);
		if (property == null || property.isEmpty()) {
			throw new SchemaException("No property "+propPath+" in new object "+objectNew+", cannot check uniqueness");
		}
		String oid = objectNew.getOid();

		ObjectQuery query;
		if (property.getDefinition() != null && QNameUtil.match(PolyStringType.COMPLEX_TYPE, property.getDefinition().getTypeName())) {
			List<PrismPropertyValue<T>> clonedValues = (List<PrismPropertyValue<T>>) PrismValueCollectionsUtil.cloneCollection(property.getValues());
			query = prismContext.queryFor(objectNew.getCompileTimeClass())
						.item(property.getPath())
						.eq(clonedValues)
						.matchingOrig()
					.or()
						.item(property.getPath())
						.eq(clonedValues)
						.matchingNorm()
					.build();
		} else {
			query = prismContext.queryFor(objectNew.getCompileTimeClass())
				.itemAs(property)
				.build();
		}

		List<PrismObject<AH>> foundObjects = repositoryService.searchObjects(objectNew.getCompileTimeClass(), query, null, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Uniqueness check of {}, property {} resulted in {} results, using query:\n{}",
					objectNew, propPath, foundObjects.size(), query.debugDump());
		}
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			LOGGER.trace("Found more than one object with property "+propPath+" = " + property);
			message("Found more than one object with property "+propPath+" = " + property);
			return false;
		}

		LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
		boolean match = foundObjects.get(0).getOid().equals(oid);
		if (!match) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found conflicting existing object with property "+propPath+" = " + property + ":\n"
						+ foundObjects.get(0).debugDump());
			}
			message("Found conflicting existing object with property "+propPath+" = " + property + ": "
					+ foundObjects.get(0));

			conflictingObject = foundObjects.get(0);
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
		Cache.enter(cacheThreadLocal, Cache.class, configuration, LOGGER);
	}

	public static void exitCache() {
		Cache.exit(cacheThreadLocal, LOGGER);
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
				clearCacheForValues(itemDelta.getValuesToAdd());			// these may present a conflict
				clearCacheForValues(itemDelta.getValuesToReplace());		// so do these
			}
		}
	}

	public static class Cache extends AbstractThreadLocalCache {

		private Set<String> conflictFreeNames = new HashSet<>();

		public static boolean isOk(PolyStringType name, CacheConfigurationManager cacheConfigurationManager) {
			if (name == null) {
				log("Null name", false);
				return false;			// strange case
			}

			CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;
			Cache cache = getCache();
			CacheConfiguration configuration = cache != null ? cache.getConfiguration() :
					cacheConfigurationManager.getConfiguration(CacheType.LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE);
			CacheConfiguration.CacheObjectTypeConfiguration objectTypeConfiguration = configuration != null ?
					configuration.getForObjectType(FocusType.class) : null;
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
			return cacheThreadLocal.get();
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

		private static void log(String message, boolean info, Object... params) {
			CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
		}
	}
}
