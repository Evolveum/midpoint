/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportConsumerWorker extends AbstractWriterConsumerWorker<ExportOptions> {

    private PrismSerializer<String> serializer;

    public ExportConsumerWorker(NinjaContext context, ExportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        serializer = context.getPrismContext()
            .xmlSerializer()
            .options(SerializationOptions.createSerializeForExport());
    }

    @Override
    protected String getProlog() {
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected <O extends ObjectType> void write(Writer writer, PrismObject<O> object) throws SchemaException, IOException {
        String xml = serializer.serialize(object);
        writer.write(xml);
    }

    @Override
    protected String getEpilog() {
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }

}
