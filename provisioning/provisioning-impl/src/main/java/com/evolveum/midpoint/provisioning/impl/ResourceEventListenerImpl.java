/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedExternalChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExternalResourceObjectChange;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.ChangeProcessingBeans;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ResourceEventListenerImpl implements ResourceEventListener {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceEventListenerImpl.class);

    @Autowired private ShadowsFacade shadowsFacade;
    @Autowired private ChangeProcessingBeans changeProcessingBeans;
    @Autowired private ProvisioningContextFactory provisioningContextFactory;
    @Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private ResourceObjectConverter resourceObjectConverter;

    private final AtomicInteger currentSequenceNumber = new AtomicInteger(0);

    @PostConstruct
    public void registerForResourceObjectChangeNotifications() {
        changeNotificationDispatcher.registerNotificationListener(this);
    }

    @PreDestroy
    public void unregisterForResourceObjectChangeNotifications() {
        changeNotificationDispatcher.unregisterNotificationListener(this);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO clean up this!
    @Override
    public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult result)
            throws CommonException, GenericConnectorException {

        Validate.notNull(eventDescription, "Event description must not be null.");
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(result, "Operation result must not be null");

        LOGGER.trace("Received event notification with the description: {}", eventDescription.debugDumpLazily());

        if (eventDescription.getResourceObject() == null && eventDescription.getObjectDelta() == null) {
            throw new IllegalStateException("Neither current shadow, nor delta specified. It is required to have at least one of them specified.");
        }

        applyDefinitions(eventDescription, result);

        PrismObject<ShadowType> anyShadow = getAnyShadow(eventDescription);
        ProvisioningContext ctx = provisioningContextFactory.create(anyShadow, task, result);
        ctx.assertDefinition();

        Object primaryIdentifierRealValue = getPrimaryIdentifierRealValue(anyShadow, eventDescription);
        Collection<ResourceAttribute<?>> identifiers = emptyIfNull(ShadowUtil.getAllIdentifiers(anyShadow));

        ExternalResourceObjectChange resourceObjectChange = new ExternalResourceObjectChange(
                currentSequenceNumber.getAndIncrement(),
                primaryIdentifierRealValue, identifiers,
                getResourceObject(eventDescription),
                eventDescription.getObjectDelta(), ctx, resourceObjectConverter);
        resourceObjectChange.initialize(task, result);

        ShadowedExternalChange adoptedChange = new ShadowedExternalChange(resourceObjectChange, false, changeProcessingBeans);
        adoptedChange.initialize(task, result);

        if (adoptedChange.isPreprocessed()) {
            ResourceObjectShadowChangeDescription shadowChangeDescription = adoptedChange.getShadowChangeDescription();
            changeNotificationDispatcher.notifyChange(shadowChangeDescription, task, result);
        } else if (adoptedChange.getProcessingState().getExceptionEncountered() != null) {
            // Currently we do very simple error handling: throw any exception to the client!
            Throwable t = adoptedChange.getProcessingState().getExceptionEncountered();
            if (t instanceof CommonException) {
                throw (CommonException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new SystemException(t);
            }
        } else if (adoptedChange.getProcessingState().isSkipFurtherProcessing()) {
            LOGGER.debug("Change is not applicable:\n{}", adoptedChange.debugDumpLazily());
            result.recordNotApplicable();
        } else {
            throw new AssertionError();
        }
    }

    private Object getPrimaryIdentifierRealValue(PrismObject<ShadowType> shadow, ResourceEventDescription context) throws SchemaException {
        Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);

        Collection<Object> primaryIdentifierRealValues = new HashSet<>();
        for (ResourceAttribute<?> primaryIdentifier : emptyIfNull(primaryIdentifiers)) {
            primaryIdentifierRealValues.addAll(primaryIdentifier.getRealValues());
        }
        if (primaryIdentifierRealValues.isEmpty()) {
            throw new SchemaException("No primary identifier in " + context);
        }
        Object primaryIdentifierRealValue = primaryIdentifierRealValues.iterator().next();
        if (primaryIdentifierRealValues.size() > 1) {
            LOGGER.warn("More than one primary identifier real value in {}: {}, using the first one: {}", context,
                    primaryIdentifierRealValues, primaryIdentifierRealValue);
        }
        return primaryIdentifierRealValue;
    }

    private void applyDefinitions(ResourceEventDescription eventDescription,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (eventDescription.getResourceObject() != null) {
            shadowsFacade.applyDefinition(eventDescription.getResourceObject(), parentResult);
        }

        if (eventDescription.getOldRepoShadow() != null){
            shadowsFacade.applyDefinition(eventDescription.getOldRepoShadow(), parentResult);
        }

        if (eventDescription.getObjectDelta() != null) {
            shadowsFacade.applyDefinition(eventDescription.getObjectDelta(), null, parentResult);
        }
    }

    // consider moving back into ResourceEventDescription
    private PrismObject<ShadowType> getAnyShadow(ResourceEventDescription eventDescription) {
        PrismObject<ShadowType> shadow;
        if (eventDescription.getResourceObject() != null) {
            shadow = eventDescription.getResourceObject();
        } else if (eventDescription.getOldRepoShadow() != null) {
            shadow = eventDescription.getOldRepoShadow();
        } else if (eventDescription.getObjectDelta() != null && eventDescription.getObjectDelta().isAdd()) {
            if (eventDescription.getObjectDelta().getObjectToAdd() == null) {
                throw new IllegalStateException("Found ADD delta, but no object to add was specified.");
            }
            shadow = eventDescription.getObjectDelta().getObjectToAdd();
        } else {
            throw new IllegalStateException("Resource event description does not contain neither old shadow, nor current shadow, nor shadow in delta");
        }
        return shadow;
    }

    // consider moving into ResourceEventDescription
    private PrismObject<ShadowType> getResourceObject(ResourceEventDescription eventDescription) {
        if (eventDescription.getResourceObject() != null) {
            return eventDescription.getResourceObject();
        } else if (ObjectDelta.isAdd(eventDescription.getObjectDelta())) {
            return eventDescription.getObjectDelta().getObjectToAdd();
        } else {
            return null;
        }
    }

}
