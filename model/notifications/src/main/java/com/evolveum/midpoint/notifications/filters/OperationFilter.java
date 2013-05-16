package com.evolveum.midpoint.notifications.filters;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.handlers.BaseHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class OperationFilter extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(OperationFilter.class);

    @PostConstruct
    public void init() {
        register(EventOperationFilterType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) {

        EventOperationFilterType eventOperationFilterType = (EventOperationFilterType) eventHandlerType;

        logStart(LOGGER, event, eventHandlerType, eventOperationFilterType.getOperation());

        boolean retval = false;

        for (EventOperationType eventOperationType : eventOperationFilterType.getOperation()) {
            if (event.isOperationType(eventOperationType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}
