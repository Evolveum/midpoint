/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.ucf.api.UcfAsyncUpdateChange;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.schema.AcknowledgementSink;
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
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
 * Transforms AsyncUpdateMessageType objects to Change ones (via UcfChangeType intermediary).
 *
 * Also prepares appropriately authenticated security context. (In the future we might factor this out to a separate class.)
 */
public class TransformationalAsyncUpdateMessageListener implements AsyncUpdateMessageListener {

    private static final Trace LOGGER = TraceManager.getTrace(TransformationalAsyncUpdateMessageListener.class);

    private static final String OP_ON_MESSAGE = TransformationalAsyncUpdateMessageListener.class.getName() + ".onMessage";
    private static final String OP_ON_MESSAGE_PREPARATION = TransformationalAsyncUpdateMessageListener.class.getName() + ".onMessagePreparation";

    private static final String VAR_MESSAGE = "message";

    @NotNull private final UcfAsyncUpdateChangeListener changeListener;
    @Nullable private final Authentication authentication;
    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;

    private final AtomicInteger messagesSeen = new AtomicInteger(0);
    private final AtomicInteger changesProduced = new AtomicInteger(0);

    TransformationalAsyncUpdateMessageListener(@NotNull UcfAsyncUpdateChangeListener changeListener,
            @Nullable Authentication authentication,
            @NotNull AsyncUpdateConnectorInstance connectorInstance) {
        this.changeListener = changeListener;
        this.authentication = authentication;
        this.connectorInstance = connectorInstance;
    }

    @Override
    public void onMessage(AsyncUpdateMessageType message, AcknowledgementSink acknowledgementSink) {
        int messageNumber = messagesSeen.getAndIncrement();
        LOGGER.trace("Got message number {}: {}", messageNumber, message);

        SecurityContextManager securityContextManager = connectorInstance.getSecurityContextManager();
        Authentication oldAuthentication = securityContextManager.getAuthentication();

        try {
            securityContextManager.setupPreAuthenticatedSecurityContext(authentication);

            Task task = connectorInstance.getTaskManager().createTaskInstance(OP_ON_MESSAGE_PREPARATION);
            task.setChannel(CHANNEL_ASYNC_UPDATE_URI);
            if (authentication != null && authentication.getPrincipal() instanceof MidPointPrincipal) {
                task.setOwner(((MidPointPrincipal) authentication.getPrincipal()).getFocus().asPrismObject().clone());
            }
            Tracer tracer = connectorInstance.getTracer();

            OperationResult result = task.getResult();
            OperationResultBuilder resultBuilder = OperationResult.createFor(OP_ON_MESSAGE);
            try {

                ActivityTracingDefinitionType tracing = connectorInstance.getConfiguration()
                        .getProcessTracingConfiguration();
                if (tracing != null) {
                    int interval = defaultIfNull(tracing.getInterval(), 1);
                    boolean matches = interval > 0 && messageNumber % interval == 0;
                    if (matches) {
                        task.setTracingProfile(tracing.getTracingProfile());
                        if (tracing.getTracingPoint().isEmpty()) {
                            task.addTracingRequest(TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING);
                        } else {
                            tracing.getTracingPoint().forEach(task::addTracingRequest);
                        }
                    }
                }

                if (task.getTracingRequestedFor().contains(TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING)) {
                    resultBuilder.tracingProfile(
                            tracer.compileProfile(
                                    task.getTracingProfile(), task.getResult()));
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
                if (changeBeans.isEmpty()) {
                    acknowledgementSink.acknowledge(true, result);
                } else {
                    AcknowledgementSink aggregatedSink = createAggregatingAcknowledgeSink(acknowledgementSink, changeBeans.size());
                    for (UcfChangeType changeBean : changeBeans) {
                        // For this to work reliably, we have to run in a single thread. But that's ok.
                        // If we receive messages in multiple threads, there is no message ordering.
                        int changeSequentialNumber = changesProduced.incrementAndGet();
                        // intentionally in this order - to process changes even after failure
                        // (if listener wants to fail fast, it can throw an exception)
                        UcfAsyncUpdateChange change = createChange(changeBean, result, changeSequentialNumber, aggregatedSink);
                        changeListener.onChange(change, task, result);
                    }
                }
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Got exception while processing asynchronous message in {}", e, task);
                result.recordFatalError(e.getMessage(), e);

                int changeSequentialNumber = changesProduced.incrementAndGet();
                UcfAsyncUpdateChange change = new UcfAsyncUpdateChange(changeSequentialNumber, UcfErrorState.error(e),
                        acknowledgementSink);
                changeListener.onChange(change, task, result);
            } finally {
                result.computeStatusIfUnknown();
                // Note that tracing really works only if the processing is synchronous.
                // (Otherwise it captures only the pre-processing activities.)
                if (result.isTraced()) {
                    tracer.storeTrace(task, result, null);
                }
            }

        } finally {
            securityContextManager.setupPreAuthenticatedSecurityContext(oldAuthentication);
        }
    }

    private AcknowledgementSink createAggregatingAcknowledgeSink(AcknowledgementSink sink, int expectedReplies) {
        if (expectedReplies > 1) {
            return new AggregatingAcknowledgeSink(sink, expectedReplies);
        } else {
            return sink; // this is the usual case
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
            data = text.isEmpty() ? null :
                    getPrismContext().parserFor(text).xml().parseRealValue();
        } else {
            throw new SchemaException(
                    "Cannot apply trivial message transformation: message is not 'any data' nor AMQP one. Please "
                            + "specify transformExpression parameter");
        }
        if (data == null) {
            return Collections.emptyList();
        } else if (data instanceof UcfChangeType) {
            return Collections.singletonList((UcfChangeType) data);
        } else {
            throw new SchemaException("Cannot apply trivial message transformation: message does not contain "
                    + "UcfChangeType object (it is " + data.getClass().getName() + " instead). Please specify transformExpression parameter");
        }
    }

    @NotNull
    private UcfAsyncUpdateChange createChange(UcfChangeType changeBean, OperationResult result, int changeSequentialNumber,
            AcknowledgementSink acknowledgeSink) throws SchemaException {
        QName objectClassName = changeBean.getObjectClass();
        if (objectClassName == null) {
            throw new SchemaException("Object class name is null in " + changeBean);
        }
        ResourceSchema resourceSchema = getResourceSchema(result);
        ResourceObjectDefinition objectClassDef = resourceSchema.findDefinitionForObjectClass(objectClassName);
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
            delta = DeltaConvertor.createObjectDelta(deltaBean, getPrismContext());
        } else {
            delta = null;
        }
        setFromDefaults(changeBean.getObject(), objectClassName);
        Holder<Object> primaryIdentifierRealValueHolder = new Holder<>();
        Collection<ResourceAttribute<?>> identifiers =
                getIdentifiers(changeBean, objectClassDef, primaryIdentifierRealValueHolder);
        if (identifiers.isEmpty()) {
            throw new SchemaException("No identifiers in async update change bean " + changeBean);
        }
        boolean notificationOnly = changeBean.getObject() == null && delta == null;
        return new UcfAsyncUpdateChange(
                changeSequentialNumber,
                primaryIdentifierRealValueHolder.getValue(),
                objectClassDef.getObjectClassDefinition(),
                identifiers,
                delta,
                asPrismObject(changeBean.getObject()),
                notificationOnly,
                acknowledgeSink);
    }

    private void setFromDefaults(ShadowType object, QName objectClassName) {
        if (object != null) {
            if (object.getObjectClass() == null) {
                object.setObjectClass(objectClassName);
            }
        }
    }

    @NotNull
    private Collection<ResourceAttribute<?>> getIdentifiers(UcfChangeType changeBean, ResourceObjectDefinition ocDef,
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
                    //noinspection unchecked
                    ResourceAttributeDefinition<Object> definition =
                            (ResourceAttributeDefinition<Object>) ocDef.findAttributeDefinition(attribute.getElementName());
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
            throw new SchemaException("No primary identifier real value in " + changeBean);
        }

        primaryIdentifierRealValueHolder.setValue(primaryIdentifierRealValues.iterator().next());
        if (primaryIdentifierRealValues.size() > 1) {
            LOGGER.warn("More than one primary identifier real value in {}: {}. Using the first one: {}", changeBean,
                    primaryIdentifierRealValues, primaryIdentifierRealValueHolder.getValue());
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
        ResourceSchema repoResourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        if (repoResourceSchema != null) {
            return repoResourceSchema;
        } else {
            throw new SchemaException("No resource schema in connector instance nor in repository. Have you executed the Test Resource operation?");
        }
    }
}
