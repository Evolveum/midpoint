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

import com.evolveum.midpoint.notifications.notifiers.Notifier;
import com.evolveum.midpoint.notifications.request.NotificationRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotifierConfigurationType;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Universal mechanism to send user a notification via particular channel (mail, sms, notification window, ...).
 *
 * Expects a request that is reasonably easily processable by content generators - i.e. free of client-specific
 * artifacts (like model state, workflow process/task instance information, etc.)
 *
 * (Unfortunately, currently the request contains exactly the model state - this has to be cleaned up later.)
 *
 * @author mederly
 */

@Component
public class NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);

    private Map<QName,Notifier> notifiers = new HashMap<QName,Notifier>();

    public void registerNotifier(QName name, Notifier notifier) {
        Validate.notNull(name);
        Validate.notNull(notifier);
        notifiers.put(name, notifier);
    }

    public void notify(NotificationRequest request, NotificationConfigurationEntryType notificationConfigurationEntry) {
        notify(request, notificationConfigurationEntry, new OperationResult("dummy"));
    }

    public void notify(NotificationRequest request, NotificationConfigurationEntryType notificationConfigurationEntry, OperationResult result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing notification request " + request + " with regard to config entry " + notificationConfigurationEntry);
        }
        for (NotifierConfigurationType notifierConfiguration : notificationConfigurationEntry.getNotifier()) {
            notify(request, notificationConfigurationEntry, notifierConfiguration, result);
        }
    }

    private void notify(NotificationRequest request, NotificationConfigurationEntryType notificationConfigurationEntry, NotifierConfigurationType notifierConfiguration, OperationResult result) {
        Notifier notifier = findNotifier(notifierConfiguration.getName());
        if (notifier == null) {
            LOGGER.error("Couldn't find notifier with name " + notifierConfiguration.getName() + ", notification to " + request.getUser() + " will not be sent.");
            return;
        }
        notifier.notify(request, notificationConfigurationEntry, notifierConfiguration, result);
    }

    private Notifier findNotifier(QName name) {
        return notifiers.get(name);
    }

}
