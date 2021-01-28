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

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.adoption.AdoptedExternalChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExternalResourceObjectChange;
import com.evolveum.midpoint.provisioning.impl.sync.ChangeProcessingBeans;
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

    @Autowired private ShadowCache shadowCache;
    @Autowired private ChangeProcessingBeans changeProcessingBeans;
    @Autowired private ProvisioningContextFactory provisioningContextFactory;
    @Autowired private ChangeNotificationDispatcher notificationManager;

    private final AtomicInteger currentSequenceNumber = new AtomicInteger(0);

    @PostConstruct
    public void registerForResourceObjectChangeNotifications() {
        notificationManager.registerNotificationListener(this);
    }

    @PreDestroy
    public void unregisterForResourceObjectChangeNotifications() {
        notificationManager.unregisterNotificationListener(this);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult parentResult)
            throws CommonException, GenericConnectorException {

        Validate.notNull(eventDescription, "Event description must not be null.");
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null");

        LOGGER.trace("Received event notification with the description: {}", eventDescription.debugDumpLazily());

        if (eventDescription.getResourceObject() == null && eventDescription.getObjectDelta() == null) {
            throw new IllegalStateException("Neither current shadow, nor delta specified. It is required to have at least one of them specified.");
        }

        applyDefinitions(eventDescription, parentResult);

        PrismObject<ShadowType> shadow = getShadow(eventDescription);
        ProvisioningContext ctx = provisioningContextFactory.create(shadow, task, parentResult);
        ctx.assertDefinition();

        Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);

//        // TODO reconsider this... MID-5834 (e.g. is this OK with index-only attributes? probably not)
//        if (ctx.getCachingStrategy() == CachingStategyType.PASSIVE) {
//            if (eventDescription.getResourceObject() == null && eventDescription.getOldRepoShadow() != null && eventDescription.getObjectDelta() != null) {
//                PrismObject<ShadowType> newShadow = eventDescription.getOldRepoShadow().clone();
//                eventDescription.getObjectDelta().applyTo(newShadow);
//                eventDescription.setResourceObject(newShadow);
//            }
//        }

        Collection<Object> primaryIdentifierRealValues = new HashSet<>();
        for (ResourceAttribute<?> primaryIdentifier : emptyIfNull(primaryIdentifiers)) {
            primaryIdentifierRealValues.addAll(primaryIdentifier.getRealValues());
        }
        if (primaryIdentifierRealValues.isEmpty()) {
            throw new SchemaException("No primary identifier in " + eventDescription);
        }
        Object primaryIdentifierRealValue = primaryIdentifierRealValues.iterator().next();
        if (primaryIdentifierRealValues.size() > 1) {
            LOGGER.warn("More than one primary identifier real value in {}: {}, using the first one: {}", eventDescription,
                    primaryIdentifierRealValues, primaryIdentifierRealValue);
        }

        PrismObject<ShadowType> resourceObject = eventDescription.getResourceObject();

        Collection<ResourceAttribute<?>> identifiers = emptyIfNull(ShadowUtil.getAllIdentifiers(resourceObject));
        ExternalResourceObjectChange resourceObjectChange = new ExternalResourceObjectChange(
                currentSequenceNumber.getAndIncrement(),
                primaryIdentifierRealValue, identifiers,
                resourceObject, eventDescription.getObjectDelta());

        AdoptedExternalChange adoptedChange = new AdoptedExternalChange(resourceObjectChange, ctx, false,
                changeProcessingBeans);

        adoptedChange.preprocess(parentResult);
        ResourceObjectShadowChangeDescription shadowChangeDescription = adoptedChange.getShadowChangeDescription();

        if (eventDescription.getOldRepoShadow() != null) {
            // TODO!!!
        }
        throw new UnsupportedOperationException("Finish this");
    }

    private void applyDefinitions(ResourceEventDescription eventDescription,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (eventDescription.getResourceObject() != null) {
            shadowCache.applyDefinition(eventDescription.getResourceObject(), parentResult);
        }

        if (eventDescription.getOldRepoShadow() != null){
            shadowCache.applyDefinition(eventDescription.getOldRepoShadow(), parentResult);
        }

        if (eventDescription.getObjectDelta() != null) {
            shadowCache.applyDefinition(eventDescription.getObjectDelta(), null, parentResult);
        }
    }

    private PrismObject<ShadowType> getShadow(ResourceEventDescription eventDescription) {
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

}
