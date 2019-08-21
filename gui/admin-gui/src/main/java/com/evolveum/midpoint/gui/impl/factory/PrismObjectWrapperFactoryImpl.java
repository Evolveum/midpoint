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
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
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

import java.util.List;

/**
 * @author katka
 *
 */
@Component
public class PrismObjectWrapperFactoryImpl<O extends ObjectType> extends PrismContainerWrapperFactoryImpl<O> implements PrismObjectWrapperFactory<O> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(PrismObjectWrapperFactoryImpl.class);

	private static final String DOT_CLASS = PrismObjectWrapperFactoryImpl.class.getName() + ".";
	private static final String OPERATION_DETERMINE_VIRTUAL_CONTAINERS = DOT_CLASS + "determineVirtualContainers";

	private QName VIRTUAL_CONTAINER_COMPLEX_TYPE = new QName("VirtualContainerType");
	private QName VIRTUAL_CONTAINER = new QName("virtualContainer");

	@Autowired private GuiComponentRegistry registry;
	@Autowired protected ModelInteractionService modelInteractionService;

	public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws SchemaException {
		applySecurityConstraints(object, context);
		if (context.getObjectStatus() == null) {
			context.setObjectStatus(status);
		}

		List<VirtualContainersSpecificationType> virtualContainers = determineVirtualContainers(object.getDefinition().getTypeName(), context);
		context.setVirtualContainers(virtualContainers);

		PrismObjectWrapper<O> objectWrapper = createObjectWrapper(object, status);
		if (context.getReadOnly() != null) {
			objectWrapper.setReadOnly(context.getReadOnly().booleanValue());
		}
		context.setShowEmpty(ItemStatus.ADDED == status ? true : false);
		PrismContainerValueWrapper<O> valueWrapper = createValueWrapper(objectWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
		objectWrapper.getValues().add(valueWrapper);

		registry.registerWrapperPanel(object.getDefinition().getTypeName(), PrismContainerPanel.class);
		return objectWrapper;
		
	}
	
	@Override
	public PrismObjectValueWrapper<O> createContainerValueWrapper(PrismContainerWrapper<O> objectWrapper, PrismContainerValue<O> objectValue, ValueStatus status) {
		return new PrismObjectValueWrapperImpl<O>((PrismObjectWrapper<O>) objectWrapper, (PrismObjectValue<O>) objectValue, status);
	}
	
	public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status) {
		return new PrismObjectWrapperImpl<O>(object, status);
	}

	@Override
	public PrismContainerValueWrapper<O> createValueWrapper(PrismContainerWrapper<O> parent, PrismContainerValue<O> value, ValueStatus status, WrapperContext context) throws SchemaException {
		PrismContainerValueWrapper<O> objectValueWrapper = super.createValueWrapper(parent, value, status, context);

		if (CollectionUtils.isEmpty(context.getVirtualContainers())) {
			return objectValueWrapper;
		}

		for (VirtualContainersSpecificationType virtualContainer : context.getVirtualContainers()){

			MutableComplexTypeDefinition mCtd = getPrismContext().definitionFactory().createComplexTypeDefinition(VIRTUAL_CONTAINER_COMPLEX_TYPE);
			DisplayType display = virtualContainer.getDisplay();

			//TODO: support full polystring -> translations could be defined directly there.
			mCtd.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
			mCtd.setHelp(WebComponentUtil.getOrigStringFromPoly(display.getHelp()));
			mCtd.setRuntimeSchema(true);

			MutablePrismContainerDefinition def = getPrismContext().definitionFactory().createContainerDefinition(VIRTUAL_CONTAINER, mCtd);
			def.setMaxOccurs(1);
			def.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
			def.setDynamic(true);

			ItemWrapperFactory factory = getRegistry().findWrapperFactory(def);
			if (factory == null) {
				LOGGER.warn("Cannot find factory for {}. Skipping wrapper creation.", def);
				continue;
			}

			WrapperContext ctx = context.clone();
			ctx.setVirtualItemSpecification(virtualContainer.getItem());
			ItemWrapper iw = factory.createWrapper(objectValueWrapper, def, ctx);


			if (iw == null) {
				continue;
			}
			((List)objectValueWrapper.getItems()).add(iw);


		}

		return objectValueWrapper;
	}

	private List<VirtualContainersSpecificationType> determineVirtualContainers(QName objectType, WrapperContext context) {
		OperationResult result = context.getResult().createMinorSubresult(OPERATION_DETERMINE_VIRTUAL_CONTAINERS);
		try {
			CompiledUserProfile userProfile = modelInteractionService.getCompiledUserProfile(context.getTask(), context.getResult());
			GuiObjectDetailsSetType objectDetailsSetType = userProfile.getObjectDetails();
			if (objectDetailsSetType == null) {
				result.recordSuccess();
				return null;
			}
			List<GuiObjectDetailsPageType> detailsPages = objectDetailsSetType.getObjectDetailsPage();
			for (GuiObjectDetailsPageType detailsPage : detailsPages) {
				if (objectType == null) {
					LOGGER.trace("Object type is not known, skipping considering custom details page settings.");
					continue;
				}
				if (detailsPage.getType() == null) {
					LOGGER.trace("Object type for details page {} not know, skipping considering custom details page settings.", detailsPage);
					continue;
				}

				if (QNameUtil.match(objectType, detailsPage.getType())) {
					result.recordSuccess();
					return detailsPage.getContainer();
				}
			}
			result.recordSuccess();
			return null;
		} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
			LOGGER.error("Cannot determine virtual containers for {}, reason: {}", objectType, e.getMessage(), e);
			result.recordPartialError("Cannot determine virtual containers for " + objectType + ", reason: " + e.getMessage(), e);
			return null;
		}


	}

	/** 
	 * 
	 * @param object
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
