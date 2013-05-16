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