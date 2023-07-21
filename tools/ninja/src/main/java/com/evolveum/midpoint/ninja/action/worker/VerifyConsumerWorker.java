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
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO from design point of view handling of main writer and deltaWriter is just nasty.
 * Fix this. Currently it was moved to VerifyReporter, still not very nice.
 *
 * @author Radovan Semancik
 */
public class VerifyConsumerWorker extends AbstractWriterConsumerWorker<VerifyOptions, ObjectType> {

    private final Log log;

    private VerificationReporter reporter;

    public VerifyConsumerWorker(NinjaContext context, VerifyOptions options,
            BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);

        this.log = context.getLog();
    }

    @Override
    protected void init() {
        reporter = new VerificationReporter(options, context.getPrismContext(), context.getCharset(), log);
        reporter.setCreateDeltaFile(true);
        reporter.init();
    }

    @Override
    public void destroy() {
        reporter.destroy();
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
        PrismObject prismObject = object.asPrismObject();

        try {
            PrismObject<?> cloned = prismObject.clone();

            UpgradeValidationResult result = reporter.verify(writer, cloned);
            if (options.isStopOnCriticalError() && result.hasCritical()) {
                shouldConsumerStop();
            }
            ObjectDelta delta = prismObject.diff(cloned);

            if (delta.isEmpty()) {
                return;
            }

            writer.write(DeltaConvertor.serializeDelta(delta, DeltaConversionOptions.createSerializeReferenceNames(), "xml"));
        } catch (Exception ex) {
            log.error("Couldn't verify object {} ({})", ex, object.getName(), object.getOid());
        }
    }

    public VerifyResult getResult() {
        return reporter.getResult();
    }
}
