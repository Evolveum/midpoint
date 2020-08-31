/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.async.ChangeListener;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_ASYNC_UPDATE_URI;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *  Transforms AsyncUpdateMessageType objects to Change ones (via UcfChangeType intermediary).
 *
 *  Also prepares appropriately authenticated security context. (In the future we might factor this out to a separate class.)
 */
public class TransformationalAsyncUpdateMessageListener implements AsyncUpdateMessageListener {

    private static final Trace LOGGER = TraceManager.getTrace(TransformationalAsyncUpdateMessageListener.class);

    private static final String OP_ON_MESSAGE = TransformationalAsyncUpdateMessageListener.class.getName() + ".onMessage";
    private static final String OP_ON_MESSAGE_PREPARATION = TransformationalAsyncUpdateMessageListener.class.getName() + ".onMessagePreparation";

    private static final String VAR_MESSAGE = "message";

    @NotNull private final ChangeListener changeListener;
    @Nullable private final Authentication authentication;
    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;

    private AtomicInteger messagesSeen = new AtomicInteger(0);

    TransformationalAsyncUpdateMessageListener(@NotNull ChangeListener changeListener, @Nullable Authentication authentication,
            @NotNull AsyncUpdateConnectorInstance connectorInstance) {
        this.changeListener = changeListener;
        this.authentication = authentication;
        this.connectorInstance = connectorInstance;
    }

    @Override
    public boolean onMessage(AsyncUpdateMessageType message) throws SchemaException {
        int messageNumber = messagesSeen.getAndIncrement();
        LOGGER.trace("Got message number {}: {}", messageNumber, message);

        SecurityContextManager securityContextManager = connectorInstance.getSecurityContextManager();
        Authentication oldAuthentication = securityContextManager.getAuthentication();

        try {
            securityContextManager.setupPreAuthenticatedSecurityContext(authentication);

            Task task = connectorInstance.getTaskManager().createTaskInstance(OP_ON_MESSAGE_PREPARATION);
            task.setChannel(CHANNEL_ASYNC_UPDATE_URI);
            if (authentication != null && authentication.getPrincipal() instanceof MidPointPrincipal) {
                task.setOwner((PrismObject<FocusType>) ((MidPointPrincipal) authentication.getPrincipal()).getFocus().asPrismObject().clone());
            }
            Tracer tracer = connectorInstance.getTracer();

            OperationResult result = task.getResult();
            OperationResultBuilder resultBuilder = OperationResult.createFor(OP_ON_MESSAGE);
            try {

                ProcessTracingConfigurationType tracingConfig = connectorInstance.getConfiguration()
                        .getProcessTracingConfiguration();
                if (tracingConfig != null) {
                    int interval = defaultIfNull(tracingConfig.getInterval(), 1);
                    boolean matches = interval > 0 && messageNumber % interval == 0;
                    if (matches) {
                        task.setTracingProfile(tracingConfig.getTracingProfile());
                        if (tracingConfig.getTracingPoint().isEmpty()) {
                            task.addTracingRequest(TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING);
                        } else {
                            tracingConfig.getTracingPoint().forEach(task::addTracingRequest);
                        }
                    }
                }

                if (task.getTracingRequestedFor().contains(TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING)) {
                    TracingProfileType profile = task.getTracingProfile() != null ? task.getTracingProfile() : tracer.getDefaultProfile();
                    resultBuilder.tracingProfile(tracer.compileProfile(profile, task.getResult()));
                }

                // replace task result with the newly-built one
                result = resultBuilder.build();
                task.setResult(result);

                VariablesMap variables = new VariablesMap();
                variables.put(VAR_MESSAGE, message, AsyncUpdateMessageType.class);
                List<UcfChangeType> changeBeans;
                try {
                    ExpressionType transformExpression = connectorInstance.getTransformExpression();
                    if (transformExpression != null) {
                        changeBeans = connectorInstance.getUcfExpressionEvaluator().evaluate(transformExpression, variables,
                                SchemaConstantsGenerated.C_UCF_CHANGE, "computing UCF change from async update",
                                task, result);
                    } else {
                        changeBeans = unwrapMessage(message);
                    }
                } catch (RuntimeException | SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
                        ConfigurationException | ExpressionEvaluationException e) {
                    throw new SystemException("Couldn't evaluate message transformation expression: " + e.getMessage(), e);
                }
                boolean ok = true;
                for (UcfChangeType changeBean : changeBeans) {
                    // intentionally in this order - to process changes even after failure
                    // (if listener wants to fail fast, it can throw an exception)
                    ok = changeListener.onChange(createChange(changeBean, result), task, result) && ok;
                }
                return ok;
            } catch (Throwable t) {
                result.recordFatalError(t.getMessage(), t);
                throw t;
            } finally {
                result.computeStatusIfUnknown();
                if (result.isTraced()) {
                    tracer.storeTrace(task, result, null);
                }
            }

        } finally {
            securityContextManager.setupPreAuthenticatedSecurityContext(oldAuthentication);
        }
    }

    /**
     * Mainly for testing purposes we provide an option to simply unwrap UcfChangeType from "any data" message.
     */
    private List<UcfChangeType> unwrapMessage(AsyncUpdateMessageType message) throws SchemaException {
        Object data;
        if (message instanceof AnyDataAsyncUpdateMessageType) {
            data = ((AnyDataAsyncUpdateMessageType) message).getData();
        } else if (message instanceof Amqp091MessageType) {
            String text = new String(((Amqp091MessageType) message).getBody(), StandardCharsets.UTF_8);
            data = getPrismContext().parserFor(text).xml().parseRealValue();
        } else {
            throw new SchemaException(
                    "Cannot apply trivial message transformation: message is not 'any data' nor AMQP one. Please "
                            + "specify transformExpression parameter");
        }
        if (data instanceof UcfChangeType) {
            return Collections.singletonList((UcfChangeType) data);
        } else {
            throw new SchemaException("Cannot apply trivial message transformation: message does not contain "
                    + "UcfChangeType object (it is " + data.getClass().getName() + " instead). Please specify transformExpression parameter");
        }
    }

    private Change createChange(UcfChangeType changeBean, OperationResult result) throws SchemaException {
        QName objectClassName = changeBean.getObjectClass();
        if (objectClassName == null) {
            throw new SchemaException("Object class name is null in " + changeBean);
        }
        ResourceSchema resourceSchema = getResourceSchema(result);
        ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(objectClassName);
        if (objectClassDef == null) {
            throw new SchemaException("Object class " + objectClassName + " not found in " + resourceSchema);
        }
        ObjectDelta<ShadowType> delta;
        ObjectDeltaType deltaBean = changeBean.getObjectDelta();
        if (deltaBean != null) {
            setFromDefaults((ShadowType) deltaBean.getObjectToAdd(), objectClassName);
            if (deltaBean.getObjectType() == null) {
                deltaBean.setObjectType(ShadowType.COMPLEX_TYPE);
            }
            delta = DeltaConvertor.createObjectDelta(deltaBean, getPrismContext(), true);
        } else {
            delta = null;
        }
        setFromDefaults(changeBean.getObject(), objectClassName);
        Holder<Object> primaryIdentifierRealValueHolder = new Holder<>();
        Collection<ResourceAttribute<?>> identifiers = getIdentifiers(changeBean, objectClassDef, primaryIdentifierRealValueHolder);
        Change change = new Change(primaryIdentifierRealValueHolder.getValue(), identifiers, asPrismObject(changeBean.getObject()), delta);
        change.setObjectClassDefinition(objectClassDef);
        if (change.getCurrentResourceObject() == null && change.getObjectDelta() == null) {
            change.setNotificationOnly(true);
        }
        return change;
    }

    private void setFromDefaults(ShadowType object, QName objectClassName) {
        if (object != null) {
            if (object.getObjectClass() == null) {
                object.setObjectClass(objectClassName);
            }
        }
    }

    private Collection<ResourceAttribute<?>> getIdentifiers(UcfChangeType changeBean, ObjectClassComplexTypeDefinition ocDef,
            Holder<Object> primaryIdentifierRealValueHolder) throws SchemaException {
        Collection<ResourceAttribute<?>> rv = new ArrayList<>();
        PrismContainerValue<ShadowAttributesType> attributesPcv;
        boolean mayContainNonIdentifiers;
        if (changeBean.getIdentifiers() != null) {
            //noinspection unchecked
            attributesPcv = changeBean.getIdentifiers().asPrismContainerValue();
            mayContainNonIdentifiers = false;
        } else if (changeBean.getObject() != null) {
            //noinspection unchecked
            attributesPcv = changeBean.getObject().getAttributes().asPrismContainerValue();
            mayContainNonIdentifiers = true;
        } else if (changeBean.getObjectDelta() != null && changeBean.getObjectDelta().getChangeType() == ChangeTypeType.ADD &&
                changeBean.getObjectDelta().getObjectToAdd() instanceof ShadowType) {
            //noinspection unchecked
            attributesPcv = ((ShadowType) changeBean.getObjectDelta().getObjectToAdd()).getAttributes().asPrismContainerValue();
            mayContainNonIdentifiers = true;
        } else {
            throw new SchemaException("Change does not contain identifiers");
        }
        Set<ItemName> identifiers = ocDef.getAllIdentifiers().stream().map(ItemDefinition::getItemName).collect(Collectors.toSet());
        Set<ItemName> primaryIdentifiers = ocDef.getPrimaryIdentifiers().stream().map(ItemDefinition::getItemName).collect(Collectors.toSet());
        Set<Object> primaryIdentifierRealValues = new HashSet<>();
        for (Item<?,?> attribute : attributesPcv.getItems()) {
            if (QNameUtil.matchAny(attribute.getElementName(), identifiers)) {
                ResourceAttribute<Object> resourceAttribute;
                if (attribute instanceof ResourceAttribute) {
                    //noinspection unchecked
                    resourceAttribute = ((ResourceAttribute) attribute).clone();
                } else {
                    ResourceAttributeDefinition<Object> definition = ocDef.findAttributeDefinition(attribute.getElementName());
                    if (definition == null) {
                        throw new SchemaException("No definition of " + attribute.getElementName() + " in " + ocDef);
                    }
                    resourceAttribute = definition.instantiate();
                    for (Object realValue : attribute.getRealValues()) {
                        resourceAttribute.addRealValue(realValue);
                    }
                }
                rv.add(resourceAttribute);
                if (QNameUtil.matchAny(attribute.getElementName(), primaryIdentifiers)) {
                    primaryIdentifierRealValues.addAll(resourceAttribute.getRealValues());
                }
            } else {
                if (!mayContainNonIdentifiers) {
                    LOGGER.warn("Attribute {} is not an identifier in {} -- ignoring it", attribute, ocDef);
                }
            }
        }
        if (primaryIdentifierRealValues.isEmpty()) {
            LOGGER.warn("No primary identifier real value in {}", changeBean);
        } else if (primaryIdentifierRealValues.size() > 1) {
            LOGGER.warn("More than one primary identifier real value in {}: {}", changeBean, primaryIdentifierRealValues);
        } else {
            primaryIdentifierRealValueHolder.setValue(primaryIdentifierRealValues.iterator().next());
        }
        return rv;
    }

    private PrismContext getPrismContext() {
        return connectorInstance.getPrismContext();
    }

    private ResourceSchema getResourceSchema(OperationResult result) throws SchemaException {
        ResourceSchema schemaInConnector = connectorInstance.getResourceSchema();
        if (schemaInConnector != null) {
            return schemaInConnector;
        }
        LOGGER.warn("No schema defined in connector: {}, will try to fetch one", connectorInstance);
        String resourceOid = connectorInstance.getResourceOid();
        if (resourceOid == null) {
            throw new SchemaException("No resource schema in connector instance and resource OID is not known either. Have you executed the Test Resource operation?");
        }
        PrismObject<ResourceType> resource;
        try {
            resource = connectorInstance.getRepositoryService().getObject(ResourceType.class, resourceOid, null, result);
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Resource with OID " + resourceOid + " could not be found in " + connectorInstance + ": "
                    + e.getMessage(), e);
        }
        ResourceSchema repoResourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, connectorInstance.getPrismContext());
        if (repoResourceSchema != null) {
            return repoResourceSchema;
        } else {
            throw new SchemaException("No resource schema in connector instance nor in repository. Have you executed the Test Resource operation?");
        }
    }
}
