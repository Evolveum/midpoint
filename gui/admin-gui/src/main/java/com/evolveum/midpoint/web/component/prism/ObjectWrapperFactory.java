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

package com.evolveum.midpoint.web.component.prism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * @author Viliam Repan (lazyman)
 */
public class ObjectWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapperFactory.class);

    private static final String DOT_CLASS = ObjectWrapperFactory.class.getName() + ".";
    private static final String CREATE_CONTAINERS = DOT_CLASS + "createContainers";
    private static final String CREATE_OBJECT_WRAPPER = DOT_CLASS + "createObjectWrapper";

    private static final List<QName> CONTAINERS_TO_IGNORE = Arrays.asList(
    		 SubjectedObjectSelectorType.COMPLEX_TYPE,
    		    TriggerType.COMPLEX_TYPE,
    		    OperationExecutionType.COMPLEX_TYPE,
    		    ApprovalSchemaType.COMPLEX_TYPE,
    		    PasswordHistoryEntryType.COMPLEX_TYPE,
    		    SecurityQuestionsCredentialsType.COMPLEX_TYPE,
    		    NonceType.COMPLEX_TYPE);

    private ModelServiceLocator modelServiceLocator;

    private OperationResult result;

    public ObjectWrapperFactory(ModelServiceLocator modelServiceLocator) {
        Validate.notNull(modelServiceLocator, "Service locator must not be null");

        this.modelServiceLocator = modelServiceLocator;
    }

    public OperationResult getResult() {
        return result;
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
            String description, PrismObject<O> object, ContainerStatus status, Task task) {
        return createObjectWrapper(displayName, description, object, status, AuthorizationPhaseType.REQUEST, task);
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName, String description,
			PrismObject<O> object, ContainerStatus status, AuthorizationPhaseType authorizationPhase, Task task) {
        if (authorizationPhase == null) {
            authorizationPhase = AuthorizationPhaseType.REQUEST;
        }
        try {
            OperationResult result = task.getResult();

            PrismObjectDefinition<O> objectDefinitionForEditing = modelServiceLocator.getModelInteractionService()
                    .getEditObjectDefinition(object, authorizationPhase, task, result);
            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Edit definition for {}:\n{}", object, objectDefinitionForEditing.debugDump(1));
            }
            
            if (objectDefinitionForEditing != null) {
            	object.setDefinition(objectDefinitionForEditing);
            }
            
            RefinedObjectClassDefinition objectClassDefinitionForEditing = null;
            if (isShadow(object)) {
                PrismReference resourceRef = object.findReference(ShadowType.F_RESOURCE_REF);
                PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
                Validate.notNull(resource, "No resource object in the resourceRef");
                objectClassDefinitionForEditing = modelServiceLocator.getModelInteractionService().getEditObjectClassDefinition(
                        (PrismObject<ShadowType>) object, resource, authorizationPhase, task, result);
                if (objectClassDefinitionForEditing != null) {
                	object.findOrCreateContainer(ShadowType.F_ATTRIBUTES).applyDefinition((PrismContainerDefinition) objectClassDefinitionForEditing.toResourceAttributeContainerDefinition());;
                }
                
            }
            return createObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                    objectClassDefinitionForEditing, status, result);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | SecurityViolationException ex) {
            throw new SystemException(ex);
        }
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
			String description, PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing,
			RefinedObjectClassDefinition objectClassDefinitionForEditing, ContainerStatus status) throws SchemaException {
        return createObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                objectClassDefinitionForEditing, status, null);
    }

    private <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
			String description, PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing,
			RefinedObjectClassDefinition objectClassDefinitionForEditing, ContainerStatus status,
			OperationResult result) throws SchemaException {

        if (result == null) {
            this.result = new OperationResult(CREATE_OBJECT_WRAPPER);
        } else {
            this.result = result;
        }
        
		ObjectWrapper<O> objectWrapper = new ObjectWrapper<>(displayName, description, object,
				status);

		List<ContainerWrapper<? extends Containerable>> containerWrappers = createContainerWrappers(objectWrapper, object,
				objectDefinitionForEditing, status, this.result);
		objectWrapper.setContainers(containerWrappers);

        this.result.computeStatusIfUnknown();

        objectWrapper.setResult(this.result);

        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Created object wrapper:\n{}", objectWrapper.debugDump());
        }

        return objectWrapper;
    }

    private <O extends ObjectType> List<ContainerWrapper<? extends Containerable>> createContainerWrappers(ObjectWrapper<O> oWrapper,
			PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing, ContainerStatus cStatus, OperationResult pResult) throws SchemaException {
        OperationResult result = pResult.createSubresult(CREATE_CONTAINERS);

        List<ContainerWrapper<? extends Containerable>> containerWrappers = new ArrayList<>();

        ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);
        try {
                ContainerWrapper<O> mainContainerWrapper = cwf.createContainerWrapper(object, oWrapper.getStatus(), cStatus, ItemPath.EMPTY_PATH);
                mainContainerWrapper.setDisplayName("prismContainer.mainPanelDisplayName");
                result.addSubresult(cwf.getResult());
                containerWrappers.add(mainContainerWrapper);

                addContainerWrappers(containerWrappers, oWrapper, object, null, result);
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error occurred during container wrapping", e);
            result.recordFatalError("Error occurred during container wrapping, reason: " + e.getMessage(), e);
            throw e;
        }

        containerWrappers.sort(new ItemWrapperComparator());
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        return containerWrappers;
    }


   private <O extends ObjectType, C extends Containerable> void addContainerWrappers(
			List<ContainerWrapper<? extends Containerable>> containerWrappers, ObjectWrapper<O> oWrapper,
			PrismContainer<C> parentContainer, ItemPath path, OperationResult result) throws SchemaException {

		PrismContainerDefinition<C> parentContainerDefinition = parentContainer.getDefinition();

		List<ItemPathSegment> segments = new ArrayList<>();
		if (path != null) {
			segments.addAll(path.getSegments());
		}
		ItemPath parentPath = new ItemPath(segments);
		for (ItemDefinition def : (Collection<ItemDefinition>) parentContainerDefinition.getDefinitions()) {
			if (!(def instanceof PrismContainerDefinition)) {
				continue;
			}
			if (isIgnoreContainer(def.getTypeName())) {
				continue;
			}
			
			if (def.isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(modelServiceLocator)) {
				LOGGER.trace("Skipping creating wrapper for container {} because it is experimental a experimental features are not enabled.", def.getName());
				continue;
			}

			LOGGER.trace("ObjectWrapper.createContainerWrapper processing definition: {}", def);

			PrismContainerDefinition<C> containerDef = (PrismContainerDefinition) def;

			ItemPath newPath = createPropertyPath(parentPath, containerDef.getName());

			ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);

			PrismContainer<C> prismContainer = parentContainer.findContainer(def.getName());
			
			ContainerWrapper<C>  container = createContainerWrapper(oWrapper.getObject(), oWrapper.getStatus(), prismContainer, containerDef, cwf, newPath);
			result.addSubresult(cwf.getResult());
			if (container != null) {
				containerWrappers.add(container);
			}
				
		}
	}
	
	private <O extends ObjectType, C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismObject<O> object, ContainerStatus objectStatus, PrismContainer<C> prismContainer, PrismContainerDefinition<C> containerDef, ContainerWrapperFactory cwf, ItemPath newPath) throws SchemaException{
		if (ShadowAssociationType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
			ObjectType objectType = object.asObjectable();
			ShadowType shadow;
			if (objectType instanceof ShadowType) {
				shadow = (ShadowType) objectType;
			} else {
				throw new SchemaException("Something very strange happenned. Association contianer in the " + objectType.getClass().getSimpleName() + "?");
			}
			Task task = modelServiceLocator.createSimpleTask("Load resource ref");
			//TODO: is it safe to case modelServiceLocator to pageBase?
			PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(shadow.getResourceRef(), modelServiceLocator, task, result);
			
			result.computeStatusIfUnknown();
			if (!result.isAcceptable()) {
				LOGGER.error("Cannot find resource referenced from shadow. {}", result.getMessage());
				result.recordPartialError("Could not find resource referenced from shadow.");
				return null;
			}
			
			if (prismContainer != null) {
				return (ContainerWrapper<C>) cwf.createAssociationWrapper(resource, shadow.getKind(), shadow.getIntent(), (PrismContainer<ShadowAssociationType>) prismContainer, objectStatus, ContainerStatus.MODIFYING, newPath);
			}
			prismContainer = containerDef.instantiate();
			return (ContainerWrapper<C>) cwf.createAssociationWrapper(resource, shadow.getKind(), shadow.getIntent(), (PrismContainer<ShadowAssociationType>) prismContainer, objectStatus, ContainerStatus.ADDING, newPath);
			
		}

		if (prismContainer != null) {
			return cwf.createContainerWrapper(prismContainer, objectStatus, ContainerStatus.MODIFYING, newPath);
		}
		
		prismContainer = containerDef.instantiate();
		return cwf.createContainerWrapper(prismContainer, objectStatus, ContainerStatus.ADDING, newPath);
		
	}
	
	private boolean isIgnoreContainer(QName containerDefinitionName) {
    	 for (QName container : CONTAINERS_TO_IGNORE) {
    		 if (container.equals(containerDefinitionName)){
    			 return true;
    		 }
    	 }

    	 return false;
    }

    private ItemPath createPropertyPath(ItemPath path, QName element) {
       return path.append(element);
    }

    private boolean isShadow(PrismObject object) {
        return (object.getCompileTimeClass() != null && ShadowType.class.isAssignableFrom(object
                .getCompileTimeClass()))
                || (object.getDefinition() != null && object.getDefinition().getName()
                .equals(ShadowType.COMPLEX_TYPE));
    }
}
