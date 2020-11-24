/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.AsyncProvisioningConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArtemisProvisioningTargetType;

import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ArtemisProvisioningTarget implements AsyncProvisioningTarget {

    @NotNull private final ArtemisProvisioningTargetType configuration;
    @NotNull private final AsyncProvisioningConnectorInstance connectorInstance;

    private ArtemisProvisioningTarget(@NotNull ArtemisProvisioningTargetType configuration, @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        this.configuration = configuration;
        this.connectorInstance = connectorInstance;
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(getClass().getName() + ".test");
        result.addParam("targetName", configuration.getName());
        result.recordSuccess();

//        try (Connection connection = connectionFactory.newConnection();
//                Channel channel = connection.createChannel()) {
//            LOGGER.info("Connection and channel created OK: {}", channel);
//            int messageCount = channel.queueDeclarePassive(sourceConfiguration.getQueue()).getMessageCount();
//            LOGGER.info("# of messages in queue {}: {}", sourceConfiguration.getQueue(), messageCount);
//            result.recordSuccess();
//        } catch (TimeoutException | IOException e) {
//            result.recordFatalError("Couldn't connect to AMQP queue: " + e.getMessage(), e);
//            throw new SystemException("Couldn't connect to AMQP queue: " + e.getMessage(), e);
//        }
    }

    @Override
    public String send(AsyncProvisioningRequest request,
            OperationResult result) {
        return "TODO";
    }
}
