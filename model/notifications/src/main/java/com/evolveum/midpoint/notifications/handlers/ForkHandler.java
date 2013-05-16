package com.evolveum.midpoint.notifications.handlers;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerForkType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

/**
 * @author mederly
 */
@Component
public class ForkHandler extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ForkHandler.class);

    @PostConstruct
    public void init() {
        register(EventHandlerForkType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) {

        logStart(LOGGER, event, eventHandlerType);

        EventHandlerForkType eventHandlerForkType = (EventHandlerForkType) eventHandlerType;
        for (JAXBElement<? extends EventHandlerType> branchHandlerType : eventHandlerForkType.getHandler()) {
            notificationManager.processEvent(event, branchHandlerType.getValue(), result);
        }

        logEnd(LOGGER, event, eventHandlerType, true);

        return true;
    }

}
