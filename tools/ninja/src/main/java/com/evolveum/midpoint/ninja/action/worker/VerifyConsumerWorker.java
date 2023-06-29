/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.VerifyResult;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Radovan Semancik
 */
public class VerifyConsumerWorker extends AbstractWriterConsumerWorker<VerifyOptions, ObjectType> {

    private VerificationReporter reporter;

    public VerifyConsumerWorker(NinjaContext context, VerifyOptions options,
            BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        reporter = new VerificationReporter(options, context.getPrismContext());
    }

    @Override
    public void destroy() {
    }

    @Override
    protected String getProlog() {
        return reporter.getProlog();
    }

    @Override
    protected String getEpilog() {
        return reporter.getEpilog();
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws IOException {
        PrismObject<?> prismObject = object.asPrismObject();

        reporter.verify(writer, prismObject);
    }

    public VerifyResult getResult() {
        return reporter.getResult();
    }
}
