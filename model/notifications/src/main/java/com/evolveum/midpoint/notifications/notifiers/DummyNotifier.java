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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.NotificationConstants;
import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.request.AccountNotificationRequest;
import com.evolveum.midpoint.notifications.request.NotificationRequest;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotifierConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * To be used in tests. (However, for the reasons to be included in AbstractModelIntegrationTest class,
 * it seems to have to live in src/main/java, not src/test/java folder.)
 *
 * @author mederly
 */

@Component
public class DummyNotifier implements Notifier, Dumpable {

    private static final Trace LOGGER = TraceManager.getTrace(DummyNotifier.class);

    public static final QName NAME = new QName(SchemaConstants.NS_C, "dummyNotifier");

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerNotifier(NAME, this);
    }

    public class NotificationRecord {
        NotificationRequest request;
        NotificationConfigurationEntryType entry;
        NotifierConfigurationType configuration;

        @Override
        public String toString() {
            return "NotificationRecord{" +
                    "request=" + request +
                    ", entry=" + entry +
                    ", configuration=" + configuration +
                    '}';
        }

        public NotifierConfigurationType getConfiguration() {
            return configuration;
        }

        public NotificationConfigurationEntryType getEntry() {
            return entry;
        }

        public NotificationRequest getRequest() {
            return request;
        }

        public Collection<String> getAccountsOids() {
            Set<String> oids = new HashSet<String>();
            if (request instanceof AccountNotificationRequest) {
                oids.add(((AccountNotificationRequest) request).getAccountOperationDescription().getObjectDelta().getOid());
            }
            return oids;
        }

        public ChangeType getFirstChangeType() {
            if (request instanceof AccountNotificationRequest) {
                return ((AccountNotificationRequest) request).getAccountOperationDescription().getObjectDelta().getChangeType();
            } else {
                return null;
            }
        }
    }

    public List<NotificationRecord> records = new ArrayList<NotificationRecord>();

    @Override
    public void notify(NotificationRequest request, NotificationConfigurationEntryType notificationConfigurationEntry, NotifierConfigurationType notifierConfiguration, OperationResult result) {
        NotificationRecord nr = new NotificationRecord();
        nr.request = request;
        nr.entry = notificationConfigurationEntry;
        nr.configuration = notifierConfiguration;
        records.add(nr);
        LOGGER.info("Dummy notifier was called. Request = " + request + ", config entry = " + notificationConfigurationEntry + ", notifier config = " + notifierConfiguration);
    }

    public List<NotificationRecord> getRecords() {
        return records;
    }

    public void clearRecords() {
        records.clear();
    }

    @Override
    public String dump() {
        return "DummyNotifier{records=" + records.toString() + "}";
    }
}
