/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.lens;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * @author semancik
 *
 */
public class ShadowConstraintsChecker<F extends FocusType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowConstraintsChecker.class);
	
	private LensProjectionContext accountContext;
	private LensContext<F> context;
	private PrismContext prismContext;
	private ProvisioningService provisioningService;
	private boolean satisfiesConstraints;
	private StringBuilder messageBuilder = new StringBuilder();
	private PrismObject conflictingShadow;

	public ShadowConstraintsChecker(LensProjectionContext accountContext) {
		this.accountContext = accountContext;
	}
	
	public LensProjectionContext getAccountContext() {
		return accountContext;
	}

	public void setAccountContext(LensProjectionContext accountContext) {
		this.accountContext = accountContext;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public ProvisioningService getProvisioningService() {
		return provisioningService;
	}

	public void setProvisioningService(ProvisioningService provisioningService) {
		this.provisioningService = provisioningService;
	}

	public LensContext<F> getContext() {
		return context;
	}
	
	public void setContext(LensContext<F> context) {
		this.context = context;
	}
	
	public boolean isSatisfiesConstraints() {
		return satisfiesConstraints;
	}
	
	public String getMessages() {
		return messageBuilder.toString();
	}

	public PrismObject getConflictingShadow() {
		return conflictingShadow;
	}
	public void check(OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		RefinedObjectClassDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
		PrismObject<ShadowType> accountNew = accountContext.getObjectNew();
		if (accountNew == null) {
			// This must be delete
			LOGGER.trace("No new object in projection context. Current shadow satisfy constraints");
			satisfiesConstraints = true;
			return;
		}
		
		PrismContainer<?> attributesContainer = accountNew.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			// No attributes no constraint violations
			LOGGER.trace("Current shadow does not contain attributes, skipping cheching uniqueness.");
			satisfiesConstraints = true;
			return;
		}
		
		Collection<? extends ResourceAttributeDefinition> uniqueAttributeDefs = MiscUtil.unionExtends(accountDefinition.getIdentifiers(),
				accountDefinition.getSecondaryIdentifiers());
		LOGGER.trace("Secondary IDs {}", accountDefinition.getSecondaryIdentifiers());
		for (ResourceAttributeDefinition attrDef: uniqueAttributeDefs) {
			PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getName());
			LOGGER.trace("Attempt to check uniqueness of {} (def {})", attr, attrDef);
			if (attr == null) {
				continue;
			}
			boolean unique = checkAttributeUniqueness(attr, accountDefinition, accountContext.getResource(), 
					accountContext.getOid(), context, result);
			if (!unique) {
				LOGGER.debug("Attribute {} conflicts with existing object (in {})", attr,  accountContext.getResourceShadowDiscriminator());
				if (isInDelta(attr, accountContext.getPrimaryDelta())) {
					throw new ObjectAlreadyExistsException("Attribute "+attr+" conflicts with existing object (and it is present in primary "+
							"account delta therefore no iteration is performed)");
				}
				if (accountContext.getResourceShadowDiscriminator() != null && accountContext.getResourceShadowDiscriminator().isThombstone()){
					satisfiesConstraints = true;
					return;
				}
				satisfiesConstraints = false;
				return;
			}
		}
		satisfiesConstraints = true;
	}
	
	private boolean checkAttributeUniqueness(PrismProperty<?> identifier, RefinedObjectClassDefinition accountDefinition,
			ResourceType resourceType, String oid, LensContext<F> context, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
//		QueryType query = QueryUtil.createAttributeQuery(identifier, accountDefinition.getObjectClassDefinition().getTypeName(),
//				resourceType, prismContext);
		
		List<?> identifierValues = identifier.getValues();
		if (identifierValues.isEmpty()) {
			throw new SchemaException("Empty identifier "+identifier+" while checking uniqueness of "+oid+" ("+resourceType+")");
		}

		OrFilter isNotDead = OrFilter.createOr(
				EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_DEAD, false),
				EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_DEAD, null));
		//TODO: set matching rule instead of null
		ObjectQuery query = ObjectQuery.createObjectQuery(
				AndFilter.createAnd(
						RefFilter.createReferenceEqual(ShadowType.class, ShadowType.F_RESOURCE_REF, prismContext, resourceType.getOid()),
						EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_OBJECT_CLASS, accountDefinition.getTypeName()),
						EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), identifier.getDefinition(), null, identifierValues),
						isNotDead));
		
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		List<PrismObject<ShadowType>> foundObjects = provisioningService.searchObjects(ShadowType.class, query, options, result);
		LOGGER.trace("Uniqueness check of {} resulted in {} results, using query:\n{}",
				new Object[]{identifier, foundObjects.size(), query.dump()});
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			LOGGER.trace("Found more than one object with attribute "+identifier.toHumanReadableString());
			message("Found more than one object with attribute "+identifier.toHumanReadableString());
			return false;
		} 
//		PrismProperty<Boolean> isDead = foundObjects.get(0).findProperty(AccountShadowType.F_DEAD);
//		if (isDead != null && !isDead.isEmpty() && isDead.getRealValue() != null && isDead.getRealValue() == true){
//			LOGGER.trace("Found matching accounts, but one of them is signed as dead, ignoring this match.");
//			message("Found matching accounts, but one of them is signed as dead, ignoring this match.");
//			return true;
//		}
//		
		LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
		boolean match = foundObjects.get(0).getOid().equals(oid);
		if (!match) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found conflicting existing object with attribute " + identifier.toHumanReadableString() + ":\n"
						+ foundObjects.get(0).dump());
			}
			message("Found conflicting existing object with attribute " + identifier.toHumanReadableString() + ": "
					+ foundObjects.get(0));

			LensProjectionContext foundContext = context.findProjectionContextByOid(foundObjects
					.get(0).getOid());
			if (foundContext != null) {
				if (foundContext.getResourceShadowDiscriminator() != null) {
					match = foundContext.getResourceShadowDiscriminator().isThombstone();
					LOGGER.trace("Comparing with account in other context resulted to {}", match);
				}
			}
			conflictingShadow = foundObjects.get(0);
		}
		
		return match;
	}
	
	private boolean isInDelta(PrismProperty<?> attr, ObjectDelta<ShadowType> delta) {
		if (delta == null) {
			return false;
		}
		return delta.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, attr.getName()));
	}

	private void message(String message) {
		if (messageBuilder.length() != 0) {
			messageBuilder.append(", ");
		}
		messageBuilder.append(message);
	}

}
