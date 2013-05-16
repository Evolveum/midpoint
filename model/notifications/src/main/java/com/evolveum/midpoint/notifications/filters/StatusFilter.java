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
public class StatusFilter extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(StatusFilter.class);

    @PostConstruct
    public void init() {
        register(EventStatusFilterType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) {

        EventStatusFilterType eventStatusFilterType = (EventStatusFilterType) eventHandlerType;

        logStart(LOGGER, event, eventHandlerType, eventStatusFilterType.getStatus());

        boolean retval = false;

        for (EventStatusType eventStatusType : eventStatusFilterType.getStatus()) {
            if (event.isStatusType(eventStatusType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}
