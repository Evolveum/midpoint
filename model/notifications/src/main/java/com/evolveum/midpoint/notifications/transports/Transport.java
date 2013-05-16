package com.evolveum.midpoint.notifications.transports;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author mederly
 */
public interface Transport {

    void send(Message message, String transportName, OperationResult parentResult);

}
