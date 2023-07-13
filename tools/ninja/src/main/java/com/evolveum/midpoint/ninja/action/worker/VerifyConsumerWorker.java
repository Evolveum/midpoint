/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.VerifyResult;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.IOUtils;

/**
 * TODO from design point of view handling of main writer and deltaWriter is just nasty. Fix this, rethink worker interface.
 *
 * @author Radovan Semancik
 */
public class VerifyConsumerWorker extends AbstractWriterConsumerWorker<VerifyOptions, ObjectType> {

    private VerificationReporter reporter;

    private Writer deltaWriter;

    public VerifyConsumerWorker(NinjaContext context, VerifyOptions options,
            BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        reporter = new VerificationReporter(options, context.getPrismContext());

        if (options.getOutput() != null && VerifyOptions.ReportStyle.CSV.equals(options.getReportStyle())) {
            try {
                File deltaFile = new File(options.getOutput() + ".delta.xml");
                if (deltaFile.exists()) {
                    deltaFile.delete();
                }

                deltaFile.createNewFile();

                deltaWriter = new FileWriter(deltaFile, context.getCharset());
                deltaWriter.write(
                        "<deltas "
                                + "xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/api-types-3\" "
                                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                                + "xsi:type=\"ObjectDeltaListType\">\n");
            } catch (IOException ex) {
                // todo handle exception
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        try {
            deltaWriter.write("</deltas>\n");
        } catch (IOException ex) {
            // todo handle exception
            ex.printStackTrace();
        }
        IOUtils.closeQuietly(deltaWriter);
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

            reporter.verify(writer, cloned);
            ObjectDelta delta = prismObject.diff(cloned);

            if (delta.isEmpty()) {
                return;
            }

            writer.write(DeltaConvertor.serializeDelta(delta, DeltaConversionOptions.createSerializeReferenceNames(), "xml"));
        } catch (Exception ex) {
            // todo handle exception
            ex.printStackTrace();
        }
    }

    public VerifyResult getResult() {
        return reporter.getResult();
    }
}
