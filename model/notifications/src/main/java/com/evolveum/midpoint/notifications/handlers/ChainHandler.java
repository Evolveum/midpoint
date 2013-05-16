package com.evolveum.midpoint.notifications.handlers;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerChainType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

/**
 * @author mederly
 */
@Component
public class ChainHandler extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ChainHandler.class);

    @PostConstruct
    public void init() {
        register(EventHandlerChainType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) {

        logStart(LOGGER, event, eventHandlerType);

        EventHandlerChainType eventHandlerChainType = (EventHandlerChainType) eventHandlerType;

        boolean shouldContinue = true;
        for (JAXBElement<? extends EventHandlerType> branchHandlerType : eventHandlerChainType.getHandler()) {
            shouldContinue = notificationManager.processEvent(event, branchHandlerType.getValue(), result);
            if (!shouldContinue) {
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, shouldContinue);
        return shouldContinue;
    }
}
