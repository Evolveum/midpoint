/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValuePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectWrapperImpl;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 *
 */
@Component
public class PrismObjectWrapperFactoryImpl<O extends ObjectType> extends PrismContainerWrapperFactoryImpl<O> implements PrismObjectWrapperFactory<O> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(PrismObjectWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistry registry;
	@Autowired protected ModelInteractionService modelInteractionService;

	public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws SchemaException {
		applySecurityConstraints(object, context);
		if (context.getObjectStatus() == null) {
			context.setObjectStatus(status);
		}

		PrismObjectWrapper<O> objectWrapper = createObjectWrapper(object, status);
		if (context.getReadOnly() != null) {
			objectWrapper.setReadOnly(context.getReadOnly().booleanValue());
		}
		context.setShowEmpty(ItemStatus.ADDED == status ? true : false);
		PrismContainerValueWrapper<O> valueWrapper = createValueWrapper(objectWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
		objectWrapper.getValues().add(valueWrapper);
		
		registry.registerWrapperPanel(object.getDefinition().getTypeName(), PrismObjectValuePanel.class);
		return objectWrapper;
		
	}
	
	@Override
	public PrismObjectValueWrapper<O> createContainerValueWrapper(PrismContainerWrapper<O> objectWrapper, PrismContainerValue<O> objectValue, ValueStatus status) {
		return new PrismObjectValueWrapperImpl<O>((PrismObjectWrapper<O>) objectWrapper, (PrismObjectValue<O>) objectValue, status);
	}
	
	public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status) {
		return new PrismObjectWrapperImpl<O>(object, status);
	}
	
	/** 
	 * 
	 * @param object
	 * @param phase
	 * @param task
	 * @param result
	 * 
	 * apply security constraint to the object, update wrapper context with additional information, e.g. shadow related attributes, ...
	 */
	protected void applySecurityConstraints(PrismObject<O> object, WrapperContext context) {
		AuthorizationPhaseType phase = context.getAuthzPhase();
		Task task = context.getTask();
		OperationResult result = context.getResult();
			try {
					PrismObjectDefinition<O> objectDef = modelInteractionService.getEditObjectDefinition(object, phase, task, result);
					object.applyDefinition(objectDef, true);
			} catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
						| CommunicationException | SecurityViolationException e) {
					// TODO Auto-generated catch block
					//TODO error handling
			}		
		
	}
	

	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismObjectDefinition;
	}

	
	@Override
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public int getOrder() {
		return 100;
	}

}
