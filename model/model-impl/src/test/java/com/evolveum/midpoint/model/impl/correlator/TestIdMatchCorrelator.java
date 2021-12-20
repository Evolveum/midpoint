/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;

import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests the whole {@link com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelator}
 * if it correctly resolves users based on interaction with external ID Match service.
 *
 * Not a part of test suite. Expects external ID Match service being available.
 *
 * FIXME Not finished yet
 */
public class TestIdMatchCorrelator extends AbstractCorrelatorOrMatcherTest {

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;

    private static final String URL = ""; // TODO
    private static final String USERNAME = ""; // TODO
    private static final String PASSWORD = ""; // TODO

    private Correlator correlator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        correlator = correlatorFactoryRegistry.instantiateCorrelator(getCorrelatorConfiguration(), initTask, initResult);
    }

    private IdMatchCorrelatorType getCorrelatorConfiguration() {
        return new IdMatchCorrelatorType(PrismContext.get())
                .url(URL)
                .username(USERNAME)
                .password(ProtectedStringType.fromClearValue(PASSWORD));
    }

    /** Temporary. */
    @Test
    public void test001Sanity() {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ShadowType resourceObject = new ShadowType(PrismContext.get())
                .beginAttributes()
                .end();

        CorrelationResult correlationResult = correlator.correlate(resourceObject, task, result);

        then();
        displayDumpable("correlation result", correlationResult);
        assertThat(correlationResult.getStatus())
                .isEqualTo(CorrelationResult.Status.UNCERTAIN);
        assertThat(correlationResult.getOwnerRef())
                .isNull();
    }
}
