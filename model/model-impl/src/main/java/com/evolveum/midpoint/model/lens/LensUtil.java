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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.ModelCompiletimeConfig;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
public class LensUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);
	
	public static <F extends ObjectType, P extends ObjectType> void traceContext(Trace logger, String activity, String phase, LensContext<F,P> context, boolean showTriples) throws SchemaException {
    	if (logger.isDebugEnabled()) {
    		StringBuilder sb = new StringBuilder("Lens context ");
    		sb.append(activity);
    		sb.append(" after ");
    		sb.append(phase);
    		sb.append(":");
    		boolean empty = true;
    		for (ObjectDelta objectDelta: context.getAllChanges()) {
    			if (objectDelta.isEmpty()) {
    				continue;
    			}
    			sb.append("\n");
    			sb.append(objectDelta.toString());
    			empty = false;
    		}
    		if (empty) {
    			sb.append(" no change");
    		}
    		logger.debug(sb.toString());
    	}
        if (logger.isTraceEnabled()) {
        	logger.trace("Lens context:\n"+
            		"---[ {} CONTEXT after {} ]--------------------------------\n"+
            		"{}\n",
            		new Object[]{activity, phase, context.dump(showTriples)});
        }
    }
	
	public static <F extends ObjectType, P extends ObjectType> ResourceType getResource(LensContext<F,P> context,
			String resourceOid, ProvisioningService provisioningService, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = context.getResource(resourceOid);
		if (resource == null) {
			// Fetching from provisioning to take advantage of caching and
			// pre-parsed schema
			resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result)
					.asObjectable();
			context.rememberResource(resource);
		}
		return resource;
	}
	
	public static String refineAccountType(String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		RefinedAccountDefinition accountDefinition = refinedSchema.getAccountDefinition(intent);
		if (accountDefinition == null) {
			throw new SchemaException("No account definition for intent="+intent+" in "+resource);
		}
		return accountDefinition.getAccountTypeName();
	}
	
	public static LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			String resourceOid, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountType);
		return getOrCreateAccountContext(context, rsd);
	}
		
	public static LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			ResourceShadowDiscriminator rsd) {
		LensProjectionContext<AccountShadowType> accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext == null) {
			accountSyncContext = context.createProjectionContext(rsd);
			ResourceType resource = context.getResource(rsd.getResourceOid());
			accountSyncContext.setResource(resource);
		}
		return accountSyncContext;
	}
	
	public static <F extends ObjectType, P extends ObjectType> LensContext<F, P> objectDeltaToContext(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ProvisioningService provisioningService, 
			PrismContext prismContext, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ObjectDelta<F> focusDelta = null;
		Collection<ObjectDelta<P>> projectionDeltas = new ArrayList<ObjectDelta<P>>(deltas.size());
		Class<F> focusClass = null;
		Class<P> projectionClass = null;
		// Sort deltas to focus and projection deltas, check if the classes are correct;
		for (ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> typeClass = delta.getObjectTypeClass();
			Validate.notNull(typeClass, "Object type class is null in "+delta);
			if (isFocalClass(typeClass)) {
				if (ModelCompiletimeConfig.CONSISTENCY_CHECKS) {
					// Focus delta has to be complete with all the definition already in place
					delta.checkConsistence(false, true, true);
				}
				focusClass = (Class<F>) typeClass;
				projectionClass = checkProjectionClass(projectionClass, (Class<P>) getProjectionClass(focusClass));
				Validate.notNull(projectionClass, "No projection class for focus "+focusClass);
				focusDelta = (ObjectDelta<F>) delta;
			} else {
				// This must be projection delta
				projectionClass = checkProjectionClass(projectionClass, (Class<P>) typeClass);
				projectionDeltas.add((ObjectDelta<P>) delta);
			}
		}
		
		LensContext<F, P> context = null;
		context = new LensContext<F, P>(focusClass, projectionClass, prismContext);
		if (focusDelta != null) {
			LensFocusContext<F> focusContext = context.createFocusContext();
			focusContext.setPrimaryDelta(focusDelta);
		}
		for (ObjectDelta<P> projectionDelta: projectionDeltas) {
			LensProjectionContext<P> projectionContext = context.createProjectionContext();
			projectionContext.setPrimaryDelta(projectionDelta);
			if (!projectionDelta.hasCompleteDefinition()) {
				// We are little bit more liberal regarding projection deltas. 
				if (ResourceObjectShadowType.class.isAssignableFrom(projectionClass)) {
					// If the deltas represent shadows we tolerate missing attribute definitions.
					// We try to add the definitions by calling provisioning
					provisioningService.applyDefinition((ObjectDelta<? extends ResourceObjectShadowType>)projectionDelta, result);
				} else {
					// This check will fail giving a better information what's wrong then just throwing an exception here.
					projectionDelta.checkConsistence(false, true, true);
				}
			}
			if (projectionDelta instanceof ShadowDiscriminatorObjectDelta) {
				ShadowDiscriminatorObjectDelta<P> shadowDelta = (ShadowDiscriminatorObjectDelta<P>)projectionDelta;
				projectionContext.setResourceShadowDiscriminator(shadowDelta.getDiscriminator());
			}
		}

		context.setFresh(false);
		
		if (ModelCompiletimeConfig.CONSISTENCY_CHECKS) context.checkConsistence();
		
		return context;
	}

	private static <T extends ObjectType> Class<T> checkProjectionClass(Class<T> oldProjectionClass, Class<T> newProjectionClass) {
		if (oldProjectionClass == null) {
			return newProjectionClass;
		} else {
			if (oldProjectionClass != oldProjectionClass) {
				throw new IllegalArgumentException("Mixed projection classes in the deltas, got both "+oldProjectionClass+" and "+oldProjectionClass);
			}
			return oldProjectionClass;
		}
	}

	public static <T extends ObjectType> boolean isFocalClass(Class<T> clazz) {
		// TODO!!!!!!!!!!!!
		if (UserType.class.isAssignableFrom(clazz)) {
			return true;
		}
		return false;
	}
	
	public static <F extends ObjectType, P extends ObjectType> Class<P> getProjectionClass(Class<F> focusClass) {
		// TODO!!!!!!!!!!!!
		if (UserType.class.isAssignableFrom(focusClass)) {
			return (Class<P>) AccountShadowType.class;
		}
		return null;
	}

}
