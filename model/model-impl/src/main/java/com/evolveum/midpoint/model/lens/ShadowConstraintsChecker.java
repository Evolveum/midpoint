/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import java.util.Collection;
import java.util.List;

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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * @author semancik
 *
 */
public class ShadowConstraintsChecker {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowConstraintsChecker.class);
	
	private LensProjectionContext<ResourceObjectShadowType> accountContext;
	private LensContext<UserType, ResourceObjectShadowType> context;
	private PrismContext prismContext;
	private RepositoryService repositoryService;
	private boolean satisfiesConstraints;
	private StringBuilder messageBuilder = new StringBuilder();
	private PrismObject conflictingShadow;

	public ShadowConstraintsChecker(LensProjectionContext<ResourceObjectShadowType> accountContext) {
		this.accountContext = accountContext;
	}
	
	public LensProjectionContext<ResourceObjectShadowType> getAccountContext() {
		return accountContext;
	}

	public void setAccountContext(LensProjectionContext<ResourceObjectShadowType> accountContext) {
		this.accountContext = accountContext;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public LensContext<UserType, ResourceObjectShadowType> getContext() {
		return context;
	}
	
	public void setContext(LensContext<UserType, ResourceObjectShadowType> context) {
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
	public void check(OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
		
		RefinedObjectClassDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
		PrismObject<ResourceObjectShadowType> accountNew = accountContext.getObjectNew();
		if (accountNew == null) {
			// This must be delete
			LOGGER.trace("No new object in projection context. Current shadow satisfy constraints");
			satisfiesConstraints = true;
			return;
		}
		
		PrismContainer<?> attributesContainer = accountNew.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
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
			ResourceType resourceType, String oid, LensContext<UserType, ResourceObjectShadowType> context, OperationResult result) throws SchemaException {
//		QueryType query = QueryUtil.createAttributeQuery(identifier, accountDefinition.getObjectClassDefinition().getTypeName(),
//				resourceType, prismContext);
		
		List<?> identifierValues = identifier.getValues();
		Validate.notEmpty(identifierValues, "Empty identifiers while checking uniqueness of "+context);
		
		OrFilter isNotDead = OrFilter.createOr(EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext, ResourceObjectShadowType.F_DEAD, false),
				EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext, ResourceObjectShadowType.F_DEAD, null));
		ObjectQuery query = ObjectQuery.createObjectQuery(
				AndFilter.createAnd(
						RefFilter.createReferenceEqual(ResourceObjectShadowType.class, ResourceObjectShadowType.F_RESOURCE_REF, prismContext, resourceType.getOid()),
						EqualsFilter.createEqual(new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES), identifier.getDefinition(), identifierValues),
						isNotDead));
		
		List<PrismObject<ResourceObjectShadowType>> foundObjects = repositoryService.searchObjects(ResourceObjectShadowType.class, query, result);
		LOGGER.trace("Uniqueness check of {} resulted in {} results, using query:\n{}",
				new Object[]{identifier, foundObjects.size(), query.dump()});
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			LOGGER.trace("Found more than one object with attribute "+identifier.getHumanReadableDump());
			message("Found more than one object with attribute "+identifier.getHumanReadableDump());
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
				LOGGER.trace("Found conflicting existing object with attribute " + identifier.getHumanReadableDump() + ":\n"
						+ foundObjects.get(0).dump());
			}
			message("Found conflicting existing object with attribute " + identifier.getHumanReadableDump() + ": "
					+ foundObjects.get(0));

			LensProjectionContext<ResourceObjectShadowType> foundContext = context.findProjectionContextByOid(foundObjects
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
	
	private boolean isInDelta(PrismProperty<?> attr, ObjectDelta<ResourceObjectShadowType> delta) {
		if (delta == null) {
			return false;
		}
		return delta.hasItemDelta(new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES, attr.getName()));
	}

	private void message(String message) {
		if (messageBuilder.length() != 0) {
			messageBuilder.append(", ");
		}
		messageBuilder.append(message);
	}

}
