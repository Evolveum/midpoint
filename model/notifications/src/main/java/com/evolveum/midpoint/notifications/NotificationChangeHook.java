/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.notifications.request.NotificationRequest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * One of interfaces of the notifier to midPoint.
 *
 * CURRENTLY A BIT OBSOLETE (and not used at this moment). Account changes are listened to using AccountOperationListener (at the level of provisioning).
 *
 * @author mederly
 */
@Component
public class NotificationChangeHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationChangeHook.class);

    public static final String HOOK_URI = "http://midpoint.evolveum.com/wf/notifier-hook-1";

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @PostConstruct
    public void init() {//        hookRegistry.registerChangeHook(HOOK_URI, this);
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Notifier change hook registered.");
//        }

    }

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {

//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Entering notifier change hook in state " + context.getState());
//        }

        // todo in the future we should perhaps act in POSTEXECUTION state, but currently the clockwork skips this state
        if (context.getState() != ModelState.FINAL) {
            return HookOperationMode.FOREGROUND;
        }

        SystemConfigurationType systemConfigurationType = NotificationsUtil.getSystemConfiguration(cacheRepositoryService, result).asObjectable();
        if (systemConfigurationType.getNotificationConfiguration() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No notification configuration, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        if (context.getFocusContext() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus context is null, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        PrismObject object = context.getFocusContext().getObjectNew();
        if (object == null) {
            object = context.getFocusContext().getObjectOld();
        }

        if (!UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus object is not a User, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        if (object == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus context object is null, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        if (context.getProjectionContexts() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No projection contexts, exiting the hook.");
            }
        }

        boolean add = false, modify = false, delete = false;

        List<ObjectDelta<ResourceObjectShadowType>> accountDeltas = new ArrayList<ObjectDelta<ResourceObjectShadowType>>();
        ModelContext<UserType, ResourceObjectShadowType> userContext = (ModelContext<UserType, ResourceObjectShadowType>) context;
        for (ModelProjectionContext<ResourceObjectShadowType> projectionContext : userContext.getProjectionContexts()) {
            ObjectDelta<ResourceObjectShadowType> delta = projectionContext.getPrimaryDelta();
            if (delta == null) {
                delta = projectionContext.getSecondaryDelta();
            }
            if (delta == null) {
                LOGGER.warn("Null account delta in projection context " + projectionContext + ", skipping it.");
                continue;
            }
            accountDeltas.add(delta);

            // second condition is brutal hack todo fixme
            //if (delta.isAdd() || (projectionContext.getObjectOld() == null && projectionContext.getObjectNew() != null)) {
            if (delta.isAdd() || SynchronizationPolicyDecision.ADD.equals(projectionContext.getSynchronizationPolicyDecision())) {
                add = true;
            } else if (delta.isModify()) {
                modify = true;
            } else if (delta.isDelete()) {
                delete = true;
            }
        }

        for (NotificationConfigurationEntryType entry : systemConfigurationType.getNotificationConfiguration().getEntry()) {
            NotificationRequest request = createRequestIfApplicable(object, entry, accountDeltas, context, add, modify, delete);
            if (request != null) {
                notificationManager.notify(request, entry);
            }
        }

        return HookOperationMode.FOREGROUND;
    }

    private NotificationRequest createRequestIfApplicable(PrismObject<UserType> user,
                                                          NotificationConfigurationEntryType entry,
                                                          List<ObjectDelta<ResourceObjectShadowType>> accountDeltas,
                                                          ModelContext<UserType,ResourceObjectShadowType> modelContext,
                                                          boolean add, boolean modify, boolean delete) {

        if ((add && entry.getSituation().contains(NotificationConstants.ACCOUNT_CREATION_QNAME)) ||
                (modify && entry.getSituation().contains(NotificationConstants.ACCOUNT_MODIFICATION_QNAME)) ||
                (delete && entry.getSituation().contains(NotificationConstants.ACCOUNT_DELETION_QNAME))) {

            NotificationRequest request = new NotificationRequest();
// todo
//            request.addParameter(NotificationConstants.ACCOUNT_DELTAS, accountDeltas);
//            request.addParameter(NotificationConstants.MODEL_CONTEXT, modelContext);
            request.setUser(user.asObjectable());
            return request;
        }
        return null;
    }
}
