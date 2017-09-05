/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.caching.AbstractCache;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 * @author mederly
 */
public class ConstraintsChecker {

	private static final Trace LOGGER = TraceManager.getTrace(ConstraintsChecker.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	private static ThreadLocal<Cache> cacheThreadLocal = new ThreadLocal<>();

	private ProvisioningContext provisioningContext;
	private PrismContext prismContext;
	private RepositoryService repositoryService;
	private ShadowCache shadowCache;
	private StringBuilder messageBuilder = new StringBuilder();
	private PrismObject<ShadowType> shadowObject;
	private String shadowOid;
	private ConstraintViolationConfirmer constraintViolationConfirmer;
	private boolean useCache = true;

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public void setProvisioningContext(ProvisioningContext provisioningContext) {
		this.provisioningContext = provisioningContext;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public ShadowCache getShadowCache() {
		return shadowCache;
	}

	public void setShadowCache(ShadowCache shadowCache) {
		this.shadowCache = shadowCache;
	}

	public void setShadowObject(PrismObject<ShadowType> shadowObject) {
		this.shadowObject = shadowObject;
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

	private ConstraintsCheckingResult constraintsCheckingResult;

	public ConstraintsCheckingResult check(Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		constraintsCheckingResult = new ConstraintsCheckingResult();
		constraintsCheckingResult.setSatisfiesConstraints(true);

		PrismContainer<?> attributesContainer = shadowObject.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			// No attributes no constraint violations
			LOGGER.trace("Current shadow does not contain attributes, skipping checking uniqueness.");
			return constraintsCheckingResult;
		}

		RefinedObjectClassDefinition objectClassDefinition = provisioningContext.getObjectClassDefinition();
		Collection<? extends ResourceAttributeDefinition> uniqueAttributeDefs = MiscUtil.unionExtends(objectClassDefinition.getPrimaryIdentifiers(),
				objectClassDefinition.getSecondaryIdentifiers());
		LOGGER.trace("Checking uniquenss of attributes: {}", uniqueAttributeDefs);
		for (ResourceAttributeDefinition attrDef: uniqueAttributeDefs) {
			PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getName());
			LOGGER.trace("Attempt to check uniqueness of {} (def {})", attr, attrDef);
			if (attr == null) {
				continue;
			}
			constraintsCheckingResult.getCheckedAttributes().add(attr.getElementName());
			boolean unique = checkAttributeUniqueness(attr, objectClassDefinition, provisioningContext.getResource(), shadowOid, task, result);
			if (!unique) {
				LOGGER.debug("Attribute {} conflicts with existing object (in {})", attr, provisioningContext.getShadowCoordinates());
				constraintsCheckingResult.getConflictingAttributes().add(attr.getElementName());
				constraintsCheckingResult.setSatisfiesConstraints(false);
			}
		}
		constraintsCheckingResult.setMessages(messageBuilder.toString());
		return constraintsCheckingResult;
	}

	private boolean checkAttributeUniqueness(PrismProperty identifier, RefinedObjectClassDefinition accountDefinition,
											 ResourceType resourceType, String oid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		List<PrismPropertyValue<?>> identifierValues = identifier.getValues();
		if (identifierValues.isEmpty()) {
			throw new SchemaException("Empty identifier "+identifier+" while checking uniqueness of "+oid+" ("+resourceType+")");
		}

		//TODO: set matching rule instead of null
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(identifier.getDefinition(), ShadowType.F_ATTRIBUTES, identifier.getDefinition().getName())
						.eq(PrismPropertyValue.cloneCollection(identifierValues))
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
		if (useCache && Cache.isOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getName(), identifier.getValues())) {
			return true;
		}

		// Here was an attempt to call cacheRepositoryService.searchObjects directly (because we use noFetch, so the net result is searching in repo anyway).
		// The idea was that it is faster and cacheable. However, it is not correct. We have to apply definition to query before execution, e.g.
		// because there could be a matching rule; see ShadowManager.processQueryMatchingRuleFilter.
		// Besides that, now the constraint checking is cached at a higher level, so this is not a big issue any more.
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		List<PrismObject<ShadowType>> foundObjects = shadowCache.searchObjects(query, options, true, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Uniqueness check of {} resulted in {} results, using query:\n{}",
					identifier, foundObjects.size(), query.debugDump());
		}
		if (foundObjects.isEmpty()) {
			if (useCache) {
				Cache.setOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getName(), identifier.getValues());
			}
			return true;
		}
		if (foundObjects.size() > 1) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Found {} objects with attribute {}", foundObjects.size() ,identifier.toHumanReadableString());
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
				Cache.setOk(resourceType.getOid(), oid, objectClassDefinition.getTypeName(), identifier.getDefinition().getName(), identifier.getValues());
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

	public static void enterCache() {
		Cache.enter(cacheThreadLocal, Cache.class, LOGGER);
	}

	public static void exitCache() {
		Cache.exit(cacheThreadLocal, LOGGER);
	}

	public static <T extends ShadowType> void onShadowAddOperation(T shadow) {
		Cache cache = Cache.getCache();
		if (cache != null) {
			log("Clearing cache on shadow add operation");
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
		ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
		for (ItemDelta itemDelta : deltas) {
			if (attributesPath.isSubPathOrEquivalent(itemDelta.getParentPath())) {
				log("Clearing cache on shadow attribute modify operation");
				cache.conflictFreeSituations.clear();
				return;
			}
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

	public static class Cache extends AbstractCache {

		private Set<Situation> conflictFreeSituations = new HashSet<>();

		private static boolean isOk(String resourceOid, String knownShadowOid, QName objectClassName, QName attributeName, List attributeValues) {
			return isOk(new Situation(resourceOid, knownShadowOid, objectClassName, attributeName, getRealValuesSet(attributeValues)));
		}

		public static boolean isOk(Situation situation) {
			if (situation.attributeValues == null) {		// special case - problem - TODO implement better
				return false;
			}

			Cache cache = getCache();
			if (cache == null) {
				log("Cache NULL for {}", situation);
				return false;
			}

			if (cache.conflictFreeSituations.contains(situation)) {
				log("Cache HIT for {}", situation);
				return true;
			} else {
				log("Cache MISS for {}", situation);
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
					return null;		// a problem!
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
			return cacheThreadLocal.get();
		}

		public static void remove(PolyStringType name) {
			Cache cache = getCache();
			if (name != null && cache != null) {
				log("Cache REMOVE for {}", name);
				cache.conflictFreeSituations.remove(name.getOrig());
			}
		}

		@Override
		public String description() {
			return "conflict-free situations: " + conflictFreeSituations;
		}
	}

	private static void log(String message, Object... params) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(message, params);
		}
		if (PERFORMANCE_ADVISOR.isTraceEnabled()) {
			PERFORMANCE_ADVISOR.trace(message, params);
		}
	}

}
