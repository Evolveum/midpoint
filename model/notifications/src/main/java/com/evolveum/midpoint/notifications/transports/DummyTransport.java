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

package com.evolveum.midpoint.notifications.transports;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class DummyTransport implements Transport {

    private static final Trace LOGGER = TraceManager.getTrace(DummyTransport.class);

    private static final String DOT_CLASS = DummyTransport.class.getName() + ".";
    public static final String NAME = "dummy";

    @Autowired
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerTransport(NAME, this);
    }

    private Map<String,List<Message>> messages = new HashMap<String,List<Message>>();

    @Override
    public void send(Message message, String name, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");

        if (!messages.containsKey(name)) {
            messages.put(name, new ArrayList<Message>());
        }
        messages.get(name).add(message);

        LOGGER.info("Recorded a message " + message + " to dummy transport buffer named " + name);

        result.recordSuccess();
    }

    public List<Message> getMessages(String transportName) {
        return messages.get(transportName);
    }

    public void clearMessages() {
        messages = new HashMap<String,List<Message>>();
    }
}