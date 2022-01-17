/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.model.test.correlator.DummyCorrelatorFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests the whole {@link com.evolveum.midpoint.model.test.correlator.DummyCorrelator}
 * if it correctly resolves users.
 *
 * FIXME Not finished yet
 */
public class TestDummyCorrelator extends AbstractCorrelatorOrMatcherTest {

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;

    private Correlator correlator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        correlator = correlatorFactoryRegistry.instantiateCorrelator(
                getCorrelatorConfiguration(),
                DummyCorrelatorFactory.CONFIGURATION_ITEM_NAME,
                initTask,
                initResult);
    }

    private AbstractCorrelatorType getCorrelatorConfiguration() {
        return new AbstractCorrelatorType(PrismContext.get());
    }

    /** Temporary. */
    @Test
    public void test001Sanity() throws SchemaException, CommunicationException {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        CorrelationResult correlationResult = correlator.correlate(new ShadowType(), task, result);

        then();
        displayDumpable("correlation result", correlationResult);
        assertThat(correlationResult.getStatus())
                .isEqualTo(CorrelationResult.Status.UNCERTAIN);
        assertThat(correlationResult.getOwnerRef())
                .isNull();
    }
}
