/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.sync.ChangeProcessor;
import com.evolveum.midpoint.provisioning.impl.sync.ProcessChangeRequest;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.HashSet;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

@Component
public class ResourceEventListenerImpl implements ResourceEventListener {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceEventListenerImpl.class);

    @Autowired private ShadowCache shadowCache;
    @Autowired private ChangeProcessor changeProcessor;
    @Autowired private ProvisioningContextFactory provisioningContextFactory;
    @Autowired private ChangeNotificationDispatcher notificationManager;

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
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException,
            GenericConnectorException, ExpressionEvaluationException {

        Validate.notNull(eventDescription, "Event description must not be null.");
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null");

        LOGGER.trace("Received event notification with the description: {}", eventDescription.debugDumpLazily());

        if (eventDescription.getCurrentResourceObject() == null && eventDescription.getDelta() == null) {
            throw new IllegalStateException("Neither current shadow, nor delta specified. It is required to have at least one of them specified.");
        }

        applyDefinitions(eventDescription, parentResult);

        PrismObject<ShadowType> shadow = eventDescription.getShadow();
        ProvisioningContext ctx = provisioningContextFactory.create(shadow, task, parentResult);
        ctx.assertDefinition();

        Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);

        // TODO reconsider this... MID-5834 (e.g. is this OK with index-only attributes? probably not)
        if (ctx.getCachingStrategy() == CachingStategyType.PASSIVE) {
            if (eventDescription.getCurrentResourceObject() == null && eventDescription.getOldRepoShadow() != null && eventDescription.getDelta() != null) {
                PrismObject<ShadowType> newShadow = eventDescription.getOldRepoShadow().clone();
                eventDescription.getDelta().applyTo(newShadow);
                eventDescription.setCurrentResourceObject(newShadow);
            }
        }

        Collection<Object> primaryIdentifierRealValues = new HashSet<>();
        for (ResourceAttribute<?> primaryIdentifier : emptyIfNull(primaryIdentifiers)) {
            primaryIdentifierRealValues.addAll(primaryIdentifier.getRealValues());
        }
        Object primaryIdentifierRealValue;
        if (primaryIdentifierRealValues.isEmpty()) {
            LOGGER.warn("No primary identifier in {}", eventDescription);
            primaryIdentifierRealValue = null;
        } else if (primaryIdentifierRealValues.size() == 1) {
            primaryIdentifierRealValue = primaryIdentifierRealValues.iterator().next();
        } else {
            LOGGER.warn("More than one primary identifier real value in {}: {}", eventDescription, primaryIdentifierRealValues);
            primaryIdentifierRealValue = null;
        }
        Change change = new Change(primaryIdentifierRealValue, primaryIdentifiers, eventDescription.getCurrentResourceObject(),
                eventDescription.getDelta(), 0); // seq number is not important here
        change.setOldRepoShadow(eventDescription.getOldRepoShadow());
        change.setObjectClassDefinition(ShadowUtil.getObjectClassDefinition(shadow));

        LOGGER.trace("Starting to synchronize change: {}", change);
        ProcessChangeRequest request = new ProcessChangeRequest(change, ctx, false);
        changeProcessor.execute(request, task, null, null, parentResult);
    }

    private void applyDefinitions(ResourceEventDescription eventDescription,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (eventDescription.getCurrentResourceObject() != null){
            shadowCache.applyDefinition(eventDescription.getCurrentResourceObject(), parentResult);
        }

        if (eventDescription.getOldRepoShadow() != null){
            shadowCache.applyDefinition(eventDescription.getOldRepoShadow(), parentResult);
        }

        if (eventDescription.getDelta() != null) {
            shadowCache.applyDefinition(eventDescription.getDelta(), null, parentResult);
        }
    }

}
