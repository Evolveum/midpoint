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

import com.evolveum.midpoint.notifications.handlers.BaseHandler;
import com.evolveum.midpoint.notifications.handlers.EventHandler;
import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.DummyNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralNotifierType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * To be used in tests. (However, for the reasons to be included in AbstractModelIntegrationTest class,
 * it seems to have to live in src/main/java, not src/test/java folder.)
 *
 * @author mederly
 */

@Component
public class DummyNotifier extends GeneralNotifier implements Dumpable {

    private static final Trace LOGGER = TraceManager.getTrace(DummyNotifier.class);

    @PostConstruct
    public void init() {
        register(DummyNotifierType.class);
    }

    public class NotificationRecord {
        Event event;

        @Override
        public String toString() {
            return "NotificationRecord{" +
                    "event=" + event +
                    '}';
        }

        public Event getEvent() {
            return event;
        }

        public Collection<String> getAccountsOids() {
            Set<String> oids = new HashSet<String>();
            if (event instanceof AccountEvent) {
                oids.add(((AccountEvent) event).getAccountOperationDescription().getObjectDelta().getOid());
            }
            return oids;
        }

        public ChangeType getFirstChangeType() {
            if (event instanceof AccountEvent) {
                return ((AccountEvent) event).getAccountOperationDescription().getObjectDelta().getChangeType();
            } else {
                return null;
            }
        }
    }

    // must be HashMap for null keys to work
    public HashMap<String,List<NotificationRecord>> records = new HashMap<String,List<NotificationRecord>>();

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        NotificationRecord nr = new NotificationRecord();
        nr.event = event;
        if (!records.containsKey(generalNotifierType.getName())) {
            records.put(generalNotifierType.getName(), new ArrayList<NotificationRecord>());
        }
        records.get(generalNotifierType.getName()).add(nr);
        LOGGER.info("Dummy notifier was called. Event = " + event);

        return false;           // skip content generation etc
    }

    public List<NotificationRecord> getRecords() {
        return getRecords(null);
    }

    public List<NotificationRecord> getRecords(String name) {
        return records.get(name);
    }

    public void clearRecords() {
        records.clear();
    }

    @Override
    public String dump() {
        return "DummyNotifier{records=" + records.toString() + "}";
    }
}
