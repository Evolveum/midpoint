/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class ObjectWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapperFactory.class);

    private static final String DOT_CLASS = ObjectWrapperFactory.class.getName() + ".";
    private static final String CREATE_CONTAINERS = DOT_CLASS + "createContainers";
    private static final String CREATE_OBJECT_WRAPPER = DOT_CLASS + "createObjectWrapper";

    private static final List<QName> INHERITED_OBJECT_SUBCONTAINERS = Arrays.asList(
            ObjectType.F_METADATA,
            ObjectType.F_EXTENSION);

    private PageBase pageBase;

    public ObjectWrapperFactory(PageBase pageBase) {
        Validate.notNull("Page parameter must not be null");

        this.pageBase = pageBase;
    }

    public <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName,
                                                                    String description,
                                                                    PrismObject<O> object,
                                                                    ContainerStatus status) {
        return createObjectWrapper(displayName, description, object, status, false);
    }

    public <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName,
                                                                    String description,
                                                                    PrismObject<O> object,
                                                                    ContainerStatus status,
                                                                    boolean delayContainerCreation) {
        try {

            OperationResult result = new OperationResult(CREATE_OBJECT_WRAPPER);
            PrismContainerDefinition objectDefinitionForEditing = pageBase.getModelInteractionService()
                    .getEditObjectDefinition(object, AuthorizationPhaseType.REQUEST, result);
            RefinedObjectClassDefinition objectClassDefinitionForEditing = null;
            if (isShadow(object)) {
                PrismReference resourceRef = object.findReference(ShadowType.F_RESOURCE_REF);
                PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
                objectClassDefinitionForEditing = pageBase.getModelInteractionService().getEditObjectClassDefinition(
                        (PrismObject<ShadowType>) object, resource, AuthorizationPhaseType.REQUEST);
            }

            return createObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                    objectClassDefinitionForEditing, status, delayContainerCreation);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    public <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName,
                                             String description,
                                             PrismObject<O> object,
                                             PrismContainerDefinition objectDefinitionForEditing,
                                             RefinedObjectClassDefinition objectClassDefinitionForEditing,
                                             ContainerStatus status,
                                             boolean delayContainerCreation) {

        ObjectWrapper wrapper = new ObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                objectClassDefinitionForEditing, status, delayContainerCreation, pageBase);

        List<ContainerWrapper> containers = createContainers(wrapper, object, objectDefinitionForEditing, status);
        wrapper.setContainers(containers);

        return wrapper;
    }

    private List<ContainerWrapper> createContainers(ObjectWrapper oWrapper, PrismObject object,
                                                    PrismContainerDefinition objectDefinitionForEditing, ContainerStatus cStatus) {
        OperationResult result = new OperationResult(CREATE_CONTAINERS);

        List<ContainerWrapper> containers = new ArrayList<>();

        try {
            Class clazz = object.getCompileTimeClass();
            if (ShadowType.class.isAssignableFrom(clazz)) {
                PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);
                ContainerStatus status = attributes != null ? cStatus : ContainerStatus.ADDING;
                if (attributes == null) {
                    PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(
                            ShadowType.F_ATTRIBUTES);
                    attributes = definition.instantiate();
                }

                ContainerWrapper container = new ContainerWrapper(oWrapper, attributes, status, new ItemPath(
                        ShadowType.F_ATTRIBUTES), pageBase);
                result.addSubresult(container.getResult());

                container.setMain(true);
                containers.add(container);

                if (hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
                        ActivationCapabilityType.class)) {
                    containers
                            .addAll(createCustomContainerWrapper(oWrapper, object, objectDefinitionForEditing, ShadowType.F_ACTIVATION, result));
                }
                if (hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
                        CredentialsCapabilityType.class)) {
                    containers
                            .addAll(createCustomContainerWrapper(oWrapper, object, objectDefinitionForEditing, ShadowType.F_CREDENTIALS, result));
                }

                PrismContainer<ShadowAssociationType> associationContainer = object
                        .findOrCreateContainer(ShadowType.F_ASSOCIATION);
                container = new ContainerWrapper(oWrapper, associationContainer, ContainerStatus.MODIFYING,
                        new ItemPath(ShadowType.F_ASSOCIATION), pageBase);
                result.addSubresult(container.getResult());
                containers.add(container);
            } else if (ResourceType.class.isAssignableFrom(clazz)) {
                containers = createResourceContainers(oWrapper, object, result);
            } else if (ReportType.class.isAssignableFrom(clazz)) {
                containers = createReportContainers(oWrapper, object, result);
            } else {
                ContainerWrapper container = new ContainerWrapper(oWrapper, object, cStatus, null, pageBase);
                result.addSubresult(container.getResult());
                containers.add(container);

                containers.addAll(createContainerWrapper(oWrapper, object, null, result));
            }
        } catch (Exception ex) {
            //TODO: shouldn't be this exception thrown????
            LoggingUtils.logUnexpectedException(LOGGER, "Error occurred during container wrapping", ex);
            result.recordFatalError("Error occurred during container wrapping, reason: " + ex.getMessage(),
                    ex);
        }

        Collections.sort(containers, new ItemWrapperComparator());
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        return containers;
    }

    public PrismContainerDefinition getDefinition(PrismObject object, PrismContainerDefinition objectDefinitionForEditing) {
        if (objectDefinitionForEditing != null) {
            return objectDefinitionForEditing;
        }
        return object.getDefinition();
    }

    private List<ContainerWrapper> createCustomContainerWrapper(ObjectWrapper oWrapper, PrismObject object,
                                                                PrismContainerDefinition objectDefinitionForEditing,
                                                                QName name, OperationResult result) {
        PrismContainer container = object.findContainer(name);
        ContainerStatus status = container == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING;
        List<ContainerWrapper> list = new ArrayList<>();
        if (container == null) {
            PrismContainerDefinition definition = getDefinition(object, objectDefinitionForEditing).findContainerDefinition(name);
            container = definition.instantiate();
        }

        ContainerWrapper wrapper = new ContainerWrapper(oWrapper, container, status, new ItemPath(name), pageBase);
        result.addSubresult(wrapper.getResult());
        list.add(wrapper);
        // list.addAll(createContainerWrapper(container, null, pageBase));
        if (!ShadowType.F_ASSOCIATION.equals(name)) {
            // [pm] is this OK? "name" is the name of the container itself; originally here was an empty path - that seems more logical
            list.addAll(createContainerWrapper(oWrapper, container, new ItemPath(name), result));
        }

        return list;
    }

    private List<ContainerWrapper> createContainerWrapper(ObjectWrapper oWrapper, PrismContainer parent, ItemPath path,
                                                          OperationResult result) {

        PrismContainerDefinition definition = parent.getDefinition();
        List<ContainerWrapper> wrappers = new ArrayList<>();

        List<ItemPathSegment> segments = new ArrayList<>();
        if (path != null) {
            segments.addAll(path.getSegments());
        }
        ItemPath parentPath = new ItemPath(segments);
        for (ItemDefinition def : (Collection<ItemDefinition>) definition.getDefinitions()) {
            if (!(def instanceof PrismContainerDefinition)) {
                continue;
            }
            if (ObjectSpecificationType.COMPLEX_TYPE.equals(def.getTypeName())) {
                continue; // TEMPORARY FIX
            }
            if (TriggerType.COMPLEX_TYPE.equals(def.getTypeName())) {
                continue; // TEMPORARY FIX TODO: remove after getEditSchema
                // (authorization) will be fixed.
            }
            if (ApprovalSchemaType.COMPLEX_TYPE.equals(def.getTypeName())) {
                continue;
            }

            LOGGER.trace("ObjectWrapper.createContainerWrapper processing definition: {}", def);

            PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
            //todo this oWrapper.isShowAssignments() value is not set when initialization occurs (only default is there) [lazyman]
            if (!oWrapper.isShowAssignments() && AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
                continue;
            }
            //todo this oWrapper.isShowInheritedObjectAttributes() value is not set when initialization occurs (only default is there) [lazyman]
            if (!oWrapper.isShowInheritedObjectAttributes()) {
                boolean res = INHERITED_OBJECT_SUBCONTAINERS.contains(containerDef.getName());
                LOGGER.info("checking " + containerDef.getName() + ", result = " + res);
                if (res) {
                    continue;
                }
            }

            ItemPath newPath = createPropertyPath(parentPath, containerDef.getName());

            // [med]
            // The following code fails to work when parent is multivalued or
            // potentially multivalued.
            // Therefore (as a brutal hack), for multivalued parents we simply
            // skip it.
            if (parent.size() <= 1) {

                // the same check as in getValue() implementation
                boolean isMultiValued = parent.getDefinition() != null && !parent.getDefinition().isDynamic()
                        && !parent.getDefinition().isSingleValue();
                if (!isMultiValued) {
                    PrismContainer prismContainer = parent.findContainer(def.getName());

                    ContainerWrapper container;
                    if (prismContainer != null) {
                        container = new ContainerWrapper(oWrapper, prismContainer, ContainerStatus.MODIFYING,
                                newPath, pageBase);
                    } else {
                        prismContainer = containerDef.instantiate();
                        container = new ContainerWrapper(oWrapper, prismContainer, ContainerStatus.ADDING,
                                newPath, pageBase);
                    }
                    result.addSubresult(container.getResult());
                    wrappers.add(container);

                    if (!AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())
                            || !ShadowType.F_ASSOCIATION.equals(parent.getElementName())) {
                        // do not show internals of Assignments (e.g. activation)
                        wrappers.addAll(createContainerWrapper(oWrapper, prismContainer, newPath, result));
                    }
                }
            }
        }

        return wrappers;
    }

    private boolean hasResourceCapability(ResourceType resource,
                                          Class<? extends CapabilityType> capabilityClass) {
        if (resource == null) {
            return false;
        }
        return ResourceTypeUtil.hasEffectiveCapability(resource, capabilityClass);
    }

    private List<ContainerWrapper> createResourceContainerWrapper(ObjectWrapper oWrapper, PrismObject object,
                                                                  PrismObject<ConnectorType> connector,
                                                                  OperationResult result) throws SchemaException {

        PrismContainer container = object.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);

        ConnectorType connectorType = connector.asObjectable();
        PrismSchema schema = ConnectorTypeUtil.parseConnectorSchema(connectorType,
                connector.getPrismContext());
        PrismContainerDefinition definition = ConnectorTypeUtil.findConfigurationContainerDefintion(
                connectorType, schema);

        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        if (container == null) {
            // brutal hack - the definition has (errorneously) set maxOccurs =
            // unbounded. But there can be only one configuration container.
            // See MID-2317 and related issues
            PrismContainerDefinition definitionFixed = definition.clone();
            definitionFixed.setMaxOccurs(1);
            container = definitionFixed.instantiate();
        }

        return createContainerWrapper(oWrapper, container, new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION), result);
    }

    private List<ContainerWrapper> createResourceContainers(ObjectWrapper oWrapper, PrismObject object,
                                                            OperationResult result) throws SchemaException {

        List<ContainerWrapper> containers = new ArrayList<>();
        PrismObject<ConnectorType> connector = loadConnector(object);

        if (connector != null) {
            containers.addAll(createResourceContainerWrapper(oWrapper, object, connector, result));
        }
        return containers;
    }

    private List<ContainerWrapper> createReportContainers(ObjectWrapper oWrapper, PrismObject object,
                                                          OperationResult result) throws SchemaException {
        List<ContainerWrapper> containers = new ArrayList<>();

        PrismContainer container = object.findContainer(ReportType.F_CONFIGURATION);
        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;

        if (container == null) {
            PrismSchema schema = ReportTypeUtil.parseReportConfigurationSchema(
                    (PrismObject<ReportType>) object, object.getPrismContext());
            PrismContainerDefinition definition = ReportTypeUtil.findReportConfigurationDefinition(schema);
            if (definition == null) {
                return containers;
            }
            container = definition.instantiate();
        }

        ContainerWrapper wrapper = new ContainerWrapper(oWrapper, container, status, new ItemPath(
                ReportType.F_CONFIGURATION), pageBase);
        result.addSubresult(wrapper.getResult());

        containers.add(wrapper);

        return containers;
    }

    private PrismObject<ConnectorType> loadConnector(PrismObject object) {
        PrismReference connectorRef = object.findReference(ResourceType.F_CONNECTOR_REF);
        return connectorRef.getValue().getObject();
        // todo reimplement
    }

    private ItemPath createPropertyPath(ItemPath path, QName element) {
        List<ItemPathSegment> segments = new ArrayList<>();
        segments.addAll(path.getSegments());
        segments.add(new NameItemPathSegment(element));

        return new ItemPath(segments);
    }

    private boolean isShadow(PrismObject object) {
        return (object.getCompileTimeClass() != null && ShadowType.class.isAssignableFrom(object
                .getCompileTimeClass()))
                || (object.getDefinition() != null && object.getDefinition().getName()
                .equals(ShadowType.COMPLEX_TYPE));
    }
}
