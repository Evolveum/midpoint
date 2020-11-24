/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import javax.annotation.PreDestroy;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 *
 */
public abstract class TestAsyncProvisioningArtemis extends TestAsyncProvisioning {

    static final boolean EXTERNAL_BROKER_TESTS_ENABLED = true;

    static final String EXTERNAL_BROKER_URL = "tcp://localhost:61616";
    static final String EXTERNAL_BROKER_LOGIN = "admin";
    static final String EXTERNAL_BROKER_PASSWORD = "secret";

    protected EmbeddedActiveMQ embeddedBroker;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        startEmbeddedBroker();
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        stopEmbeddedBroker();
    }

    private void startEmbeddedBroker() throws Exception {
        embeddedBroker = new EmbeddedActiveMQ();
        embeddedBroker.start();
    }

    private void stopEmbeddedBroker() throws Exception {
        embeddedBroker.stop();
    }

}
