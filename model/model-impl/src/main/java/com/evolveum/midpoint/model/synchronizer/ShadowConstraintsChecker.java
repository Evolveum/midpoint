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
package com.evolveum.midpoint.model.synchronizer;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * @author semancik
 *
 */
public class ShadowConstraintsChecker {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowConstraintsChecker.class);
	
	private AccountSyncContext accountContext;
	private PrismContext prismContext;
	private RepositoryService repositoryService;
	private boolean satisfiesConstraints;
	private StringBuilder messageBuilder = new StringBuilder();

	public ShadowConstraintsChecker(AccountSyncContext accountContext) {
		this.accountContext = accountContext;
	}
	
	public AccountSyncContext getAccountContext() {
		return accountContext;
	}

	public void setAccountContext(AccountSyncContext accountContext) {
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

	public boolean isSatisfiesConstraints() {
		return satisfiesConstraints;
	}
	
	public String getMessages() {
		return messageBuilder.toString();
	}

	public void check(OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
		
		RefinedAccountDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
		PrismObject<AccountShadowType> accountNew = accountContext.getAccountNew();
		if (accountNew == null) {
			// This must be delete
			satisfiesConstraints = true;
			return;
		}
		PrismContainer<?> attributesContainer = accountNew.findContainer(AccountShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			// No attributes no constraint violations
			satisfiesConstraints = true;
			return;
		}
		Collection<ResourceAttributeDefinition> uniqueAttributeDefs = MiscUtil.union(accountDefinition.getIdentifiers(),
				accountDefinition.getSecondaryIdentifiers());
		LOGGER.trace("Secondary IDs {}", accountDefinition.getSecondaryIdentifiers());
		for (ResourceAttributeDefinition attrDef: uniqueAttributeDefs) {
			PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getName());
			LOGGER.trace("Attempt to check uniqueness of {} (def {})", attr, attrDef);
			if (attr == null) {
				continue;
			}
			boolean unique = checkAttributeUniqueness(attr, accountDefinition, accountContext.getResource(), 
					accountContext.getOid(), result);
			if (!unique) {
				LOGGER.debug("Attribute {} conflicts with existing object (in {})", attr,  accountContext.getResourceAccountType());
				if (isInDelta(attr, accountContext.getAccountPrimaryDelta())) {
					throw new ObjectAlreadyExistsException("Attribute "+attr+" conflicts with existing object (and it is present in primary "+
							"account delta therefore no iteration is performed)");
				}
				satisfiesConstraints = false;
				return;
			}
		}
		satisfiesConstraints = true;
	}
	
	private boolean checkAttributeUniqueness(PrismProperty<?> identifier, RefinedAccountDefinition accountDefinition,
			ResourceType resourceType, String oid, OperationResult result) throws SchemaException {
		QueryType query = QueryUtil.createAttributeQuery(identifier, accountDefinition.getObjectClassDefinition().getTypeName(),
				resourceType, prismContext);
		List<PrismObject<AccountShadowType>> foundObjects = repositoryService.searchObjects(AccountShadowType.class, query, null, result);
		LOGGER.trace("Uniqueness check of {} resulted in {} results, using query:\n{}",
				new Object[]{identifier, foundObjects.size(), DOMUtil.serializeDOMToString(query.getFilter())});
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			message("Found more than one object with attribute "+identifier.getHumanReadableDump());
			return false;
		}
		LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
		boolean match = foundObjects.get(0).getOid().equals(oid);
		if (!match) {
			message("Found conflicting existing object with attribute "+identifier.getHumanReadableDump()+": "+foundObjects.get(0));
		}
		return match;
	}
	
	private boolean isInDelta(PrismProperty<?> attr, ObjectDelta<AccountShadowType> delta) {
		if (delta == null) {
			return false;
		}
		return delta.hasItemDelta(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES, attr.getName()));
	}

	private void message(String message) {
		if (messageBuilder.length() != 0) {
			messageBuilder.append(", ");
		}
		messageBuilder.append(message);
	}

}
