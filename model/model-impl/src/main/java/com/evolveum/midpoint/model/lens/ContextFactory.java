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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@Component
public class ContextFactory {
	
	@Autowired(required = true)
	PrismContext prismContext;

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	Protector protector;
	
	public <F extends ObjectType, P extends ObjectType> LensContext<F, P> createContext(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
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
				if (!delta.isAdd() && delta.getOid() == null) {
					throw new IllegalArgumentException("Delta "+delta+" does not have an OID");
				}
				if (focusClass == ResourceType.class && !delta.hasCompleteDefinition()) {
					// Connector schema needs to be applied to resource def to make it complete
					provisioningService.applyDefinition(delta, result);
				}
				if (InternalsConfig.consistencyChecks) {
					// Focus delta has to be complete now with all the definition already in place
					delta.checkConsistence(false, true, true);
				}
                if (focusDelta != null) {
                    throw new IllegalStateException("More than one focus delta used in model operation");
                }
				focusDelta = (ObjectDelta<F>) delta;
			} else {
				// This must be projection delta
				projectionClass = checkProjectionClass(projectionClass, (Class<P>) typeClass);
				projectionDeltas.add((ObjectDelta<P>) delta);
			}
		}
		
		if (focusClass == null) {
			focusClass = determineFocusClass(projectionClass);
		}
		LensContext<F, P> context = new LensContext<F, P>(focusClass, projectionClass, prismContext, provisioningService);
		context.setChannel(task.getChannel());
		context.setOptions(options);
		context.setDoReconciliationForAllProjections(ModelExecuteOptions.isReconcile(options));
		if (focusDelta != null) {
			LensFocusContext<F> focusContext = context.createFocusContext();
			focusContext.setPrimaryDelta(focusDelta);
		}
		for (ObjectDelta<P> projectionDelta: projectionDeltas) {
			LensProjectionContext<P> projectionContext = context.createProjectionContext();
			projectionContext.setPrimaryDelta(projectionDelta);
			// We are little bit more liberal regarding projection deltas. 
			if (ShadowType.class.isAssignableFrom(projectionClass)) {
				// If the deltas represent shadows we tolerate missing attribute definitions.
				// We try to add the definitions by calling provisioning
				provisioningService.applyDefinition((ObjectDelta<? extends ShadowType>)projectionDelta, result);
			} else {
				// This check will fail giving a better information what's wrong then just throwing an exception here.
				projectionDelta.checkConsistence(false, true, true);
			}		
			if (projectionDelta instanceof ShadowDiscriminatorObjectDelta) {
				ShadowDiscriminatorObjectDelta<P> shadowDelta = (ShadowDiscriminatorObjectDelta<P>)projectionDelta;
				projectionContext.setResourceShadowDiscriminator(shadowDelta.getDiscriminator());
			} else {
				if (!projectionDelta.isAdd() && projectionDelta.getOid() == null) {
					throw new IllegalArgumentException("Delta "+projectionDelta+" does not have an OID");
				}
			}
		}

		// This forces context reload before the next projection
		context.rot();
		
		if (InternalsConfig.consistencyChecks) context.checkConsistence();
		
		return context;
	}
	
	
	public <F extends FocusType, O extends ObjectType> LensContext<F, ShadowType> createRecomputeContext(
    		PrismObject<O> object, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		Class<O> typeClass = object.getCompileTimeClass();
		LensContext<F, ShadowType> context;
		if (isFocalClass(typeClass)) {
			context = (LensContext<F, ShadowType>) createRecomputeFocusContext((Class<FocusType>)typeClass, (PrismObject<FocusType>) object, task, result);
		} else if (ShadowType.class.isAssignableFrom(typeClass)) {
			context =  (LensContext<F, ShadowType>) createRecomputeProjectionContext((PrismObject<ShadowType>) object, task, result);
		} else {
			throw new IllegalArgumentException("Cannot create recompute context for "+object);
		}
		context.setLazyAuditRequest(true);
		return context;
	}
	
	public <F extends FocusType> LensContext<F, ShadowType> createRecomputeFocusContext(
    		Class<F> focusType, PrismObject<F> focus, Task task, OperationResult result) {
    	LensContext<F, ShadowType> syncContext = new LensContext<F, ShadowType>(focusType,
				ShadowType.class, prismContext, provisioningService);
		LensFocusContext<F> focusContext = syncContext.createFocusContext();
		focusContext.setLoadedObject(focus);
		focusContext.setOid(focus.getOid());
		syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECOMPUTE));
		syncContext.setDoReconciliationForAllProjections(true);
		return syncContext;
    }
	
	public LensContext<FocusType, ShadowType> createRecomputeProjectionContext(
    		PrismObject<ShadowType> shadow, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		provisioningService.applyDefinition(shadow, result);
    	LensContext<FocusType, ShadowType> syncContext = new LensContext<FocusType, ShadowType>(null,
				ShadowType.class, prismContext, provisioningService);
    	LensProjectionContext<ShadowType> projectionContext = syncContext.createProjectionContext();
    	projectionContext.setLoadedObject(shadow);
    	projectionContext.setOid(shadow.getOid());
    	projectionContext.setDoReconciliation(true);
		syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECOMPUTE));
		return syncContext;
    }
	
	public static <F extends ObjectType, P extends ObjectType> Class<F> determineFocusClass(Class<P> projectionClass) {
		if (projectionClass == ShadowType.class) {
			return (Class<F>) UserType.class;
		}
		return null;
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
		if (ResourceType.class.isAssignableFrom(clazz)) {
			return true;
		}
		return false;
	}
	
	public static <F extends ObjectType, P extends ObjectType> Class<P> getProjectionClass(Class<F> focusClass) {
		// TODO!!!!!!!!!!!!
		if (UserType.class.isAssignableFrom(focusClass)) {
			return (Class<P>) ShadowType.class;
		}
		if (ResourceType.class.isAssignableFrom(focusClass)) {
			// This has no projection class. But returning null will cause error. Returning the same class is harmless.
			return (Class<P>) ResourceType.class;
		}
		return null;
	}


}
