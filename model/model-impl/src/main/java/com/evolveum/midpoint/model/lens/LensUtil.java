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

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.ModelCompiletimeConfig;
import com.evolveum.midpoint.model.api.ShadowProjectionObjectDelta;
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
	
	public static LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			ResourceAccountType rat, ProvisioningService provisioningService, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		LensProjectionContext<AccountShadowType> accountSyncContext = context.findProjectionContext(rat);
		if (accountSyncContext == null) {
			ResourceType resource = context.getResource(rat);
			if (resource == null) {
				// Fetching from provisioning to take advantage of caching and
				// pre-parsed schema
				resource = provisioningService.getObject(ResourceType.class, rat.getResourceOid(), null, result)
						.asObjectable();
				context.rememberResource(resource);
			}
			accountSyncContext = context.createProjectionContext(rat);
			accountSyncContext.setResource(resource);
		}
		return accountSyncContext;
	}
	
	public static <F extends ObjectType, P extends ObjectType> LensContext<F, P> objectDeltaToContext(
			Collection<ObjectDelta<? extends ObjectType>> deltas, PrismContext prismContext) {
		ObjectDelta<F> focusDelta = null;
		Collection<ObjectDelta<P>> projectionDeltas = new ArrayList<ObjectDelta<P>>(deltas.size());
		Class<F> focusClass = null;
		Class<P> projectionClass = null;
		// Sort deltas to focus and projection deltas, check if the classes are correct;
		for (ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> typeClass = delta.getObjectTypeClass();
			Validate.notNull(typeClass, "Object type class is null in "+delta);
			if (isFocalClass(typeClass)) {
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
			if (projectionDelta instanceof ShadowProjectionObjectDelta) {
				ShadowProjectionObjectDelta<P> shadowDelta = (ShadowProjectionObjectDelta<P>)projectionDelta;
				ResourceAccountType ri = new ResourceAccountType(shadowDelta.getResourceOid(), shadowDelta.getIntent());
				projectionContext.setResourceAccountType(ri);
			}
		}

		context.setFresh(false);
		
		if (ModelCompiletimeConfig.CONSISTENCY_CHECKS) {
			context.checkConsistence();
		}
		
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
