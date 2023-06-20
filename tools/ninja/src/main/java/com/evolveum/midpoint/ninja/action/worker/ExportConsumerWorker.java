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

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportConsumerWorker extends AbstractWriterConsumerWorker<ExportOptions, ObjectType> {

    private PrismSerializer<String> serializer;

    public ExportConsumerWorker(NinjaContext context,
            ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        serializer = context.getPrismContext()
                .xmlSerializer()
                .options(SerializationOptions.createSerializeForExport().skipContainerIds(options.isSkipContainerIds()));
    }

    @Override
    protected String getProlog() {
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws SchemaException, IOException {
        String xml = serializer.serialize(object.asPrismObject());
        writer.write(xml);
    }

    @Override
    protected String getEpilog() {
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }

}
