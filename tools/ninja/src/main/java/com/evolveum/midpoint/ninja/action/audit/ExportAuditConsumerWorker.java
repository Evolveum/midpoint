/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Consumer writing exported audit events to the writer (stdout or file).
 */
public class ExportAuditConsumerWorker
        extends AbstractWriterConsumerWorker<ExportAuditOptions, AuditEventRecordType> {

    private PrismSerializer<String> serializer;

    public ExportAuditConsumerWorker(NinjaContext context,
            ExportAuditOptions options, BlockingQueue<AuditEventRecordType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        serializer = context.getPrismContext()
                .xmlSerializer()
                .options(SerializationOptions.createSerializeForExport()
                        // TODO: This does not help with RawType: (parsed:ObjectReferenceType) for which
                        //  the names still go only to the comments (ignored by the import, obviously).
                        .serializeReferenceNames(true)
                        .skipContainerIds(true));
    }

    @Override
    protected String getProlog() {
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected void write(Writer writer, AuditEventRecordType object) throws SchemaException, IOException {
        String xml = serializer.serialize(object.asPrismContainerValue());
        writer.write(xml);
    }

    @Override
    protected String getEpilog() {
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }
}
