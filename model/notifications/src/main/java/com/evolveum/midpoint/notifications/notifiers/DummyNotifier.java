/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.DummyNotifierType;
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

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
