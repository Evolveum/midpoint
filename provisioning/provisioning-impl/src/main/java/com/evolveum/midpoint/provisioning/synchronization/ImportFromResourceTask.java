/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.synchronization;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.provisioning.service.ResourceObjectShadowCache;
import com.evolveum.midpoint.provisioning.service.ResultHandler;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author semancik
 */
public class ImportFromResourceTask extends Thread implements ResultHandler {

    private static final Trace logger = TraceManager.getTrace(ImportFromResourceTask.class);

    private ResourceType resource;
    private ResourceAccessInterface rai;
    private ResourceObjectDefinition objectDefinition;
    private long progress;
    private ResourceObjectChangeListenerPortType objectChangeListener;
    private ResourceObjectShadowCache shadowCache;
    private boolean stopOnError;
    private Long finishTime;
    private Long launchTime;
    private Exception lastError;
    private long lastErrorTime;
    private long numberOfErrors;


    public ImportFromResourceTask(ResourceType resource, ResourceAccessInterface rai, ResourceObjectDefinition objectDefinition) {
        this.resource = resource;
        this.rai = rai;
        this.objectDefinition = objectDefinition;
        progress = 0;
        objectChangeListener = null;
        finishTime = null;
        launchTime = null;
        stopOnError = false;
        lastError = null;
        numberOfErrors = 0;
    }

    public long getProgress() {
        return progress;
    }


    /**
     * Get the value of objectChangeListener
     *
     * @return the value of objectChangeListener
     */
    public ResourceObjectChangeListenerPortType getObjectChangeListener() {
        return objectChangeListener;
    }

    /**
     * Set the value of objectChangeListener
     *
     * @param objectChangeListener new value of objectChangeListener
     */
    public void setObjectChangeListener(ResourceObjectChangeListenerPortType objectChangeListener) {
        this.objectChangeListener = objectChangeListener;
    }

    /**
     * Get the value of shadowCache
     *
     * @return the value of shadowCache
     */
    public ResourceObjectShadowCache getResourceObjectShadowCache() {
        return shadowCache;
    }

    /**
     * Set the value of shadowCache
     *
     * @param shadowCache new value of shadowCache
     */
    public void setResourceObjectShadowCache(ResourceObjectShadowCache shadowCache) {
        this.shadowCache = shadowCache;
    }
    
    /**
     * Get the value of stopOnError
     *
     * @return the value of stopOnError
     */
    public boolean isStopOnError() {
        return stopOnError;
    }

    /**
     * Set the value of stopOnError
     *
     * @param stopOnError new value of stopOnError
     */
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    /**
     * Get the value of launchTime
     *
     * @return the value of launchTime
     */
    public Long getLaunchTime() {
        return launchTime;
    }

    /**
     * Set the value of launchTime
     *
     * @param launchTime new value of launchTime
     */
    public void setLaunchTime(Long launchTime) {
        this.launchTime = launchTime;
    }

    /**
     * Get the value of finishTime
     *
     * @return the value of finishTime
     */
    public Long getFinishTime() {
        return finishTime;
    }

    /**
     * Set the value of finishTime
     *
     * @param finishTime new value of finishTime
     */
    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * Get the value of lastError
     *
     * @return the value of lastError
     */
    public Exception getLastError() {
        return lastError;
    }

    /**
     * Set the value of lastError
     *
     * @param lastError new value of lastError
     */
    public void setLastError(Exception lastError) {
        this.lastError = lastError;
    }

    private void recordError(Exception error) {
        lastError = error;
        lastErrorTime = System.currentTimeMillis();
        numberOfErrors++;
    }

    /**
     * Get the value of lastErrorTime
     *
     * @return the value of lastErrorTime
     */
    public long getLastErrorTime() {
        return lastErrorTime;
    }

    /**
     * Set the value of lastErrorTime
     *
     * @param lastErrorTime new value of lastErrorTime
     */
    public void setLastErrorTime(long lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    /**
     * Get the value of numberOfErrors
     *
     * @return the value of numberOfErrors
     */
    public long getNumberOfErrors() {
        return numberOfErrors;
    }

    /**
     * Set the value of numberOfErrors
     *
     * @param numberOfErrors new value of numberOfErrors
     */
    public void setNumberOfErrors(long numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }


    @Override
    public void run() {
        logger.debug("Import from resource {} starting",resource.getName());

        setLaunchTime(System.currentTimeMillis());

        OperationalResultType operationalResult = new OperationalResultType();

        try {
            // Start the search, this will iterativelly call handle(..) method
            rai.iterativeSearch(operationalResult, objectDefinition, this);

        } catch (Exception ex) {
            // We want to make sure that the thread result is recorded, even if
            // it dies in a spectacular way.
            recordError(ex);
            logger.error("The import from resource {} failed in a spectacular way: {}",new Object[]{resource.getOid(),ex.getMessage(),ex});
        }
        // TODO: remember results

        setFinishTime(System.currentTimeMillis());

        logger.debug("Import from resource {} finishing",resource.getName());
    }

    @Override
    public boolean handle(ResourceObject resourceObject) {
        progress++;
        logger.debug("Import from resource {} processing object {}, progress {}",new Object[]{resource.getName(),resourceObject,progress});

        if (objectChangeListener==null) {
            logger.warn("No object change listener set for import task, ending the task");
            return false;
        }

        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_IMPORT));
        change.setResource(resource);

        ResourceObjectShadowType oldShadow = null;
        try {
            // We need old shadow state to add it to change notification
            oldShadow = shadowCache.getCurrentShadow(resourceObject,resource);
            logger.debug("Got old state: {}",DebugUtil.prettyPrint(oldShadow));
        } catch (FaultMessage ex) {
            // Repository error. This is really not expected here.
            // So, until we figure out better error handling here, just stop
            // the import for now.
            recordError(ex);
            logger.error("Unexpected repository error: "+ex.getFaultInfo());
            return !isStopOnError();
        }

        // We need to sync the ResourceObject and the Shadow now
        ResourceObjectShadowType newShadow;
        try {
            newShadow = shadowCache.update(resourceObject, resource);
        } catch (FaultMessage ex) {
            // Repository error. This is really not expected here.
            recordError(ex);
            logger.error("Unexpected repository error: "+ex.getFaultInfo());
            return !isStopOnError();
        }

        if (newShadow==null) {
            // something strange happened here

            Exception ex = new IllegalArgumentException("The shadow from shadow cache is null");
            recordError(ex);
            logger.error("The shadow from shadow cache is null. Resource {}",ex,resource.getName());
            logger.debug("resource object: {}",resourceObject);

            return !isStopOnError();
        }

        // We are import, therefore we are going to pretend the resource objetc
        // was just created, regardless of the old IDM state

        ObjectChangeAdditionType addChange = new ObjectChangeAdditionType();
        addChange.setObject(newShadow);
        change.setObjectChange(addChange);

        // Interface contract says: provide shadow in the state as it was before
        // the change (if possible).
        if (oldShadow != null) {
            change.setShadow(oldShadow);
        } else {
            change.setShadow(newShadow);
        }

        logger.debug("Going to call notification with new object: "+DebugUtil.prettyPrint(newShadow));
        try {
            objectChangeListener.notifyChange(change);
        } catch (com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.FaultMessage ex) {
            recordError(ex);
            logger.error("Change notication listener failed for import of object {}: {}",new Object[]{resourceObject,DebugUtil.prettyPrint(ex),ex});
            return !isStopOnError();
        } catch (RuntimeException ex) {
            recordError(ex);
            logger.error("Change notication listener failed for import of object {}: {}: ",new Object[]{resourceObject,ex.getClass().getSimpleName(),ex.getMessage(),ex});
            return !isStopOnError();
        }

        return true;
    }


}
