/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

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
        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        if (shouldSkipObject(prismObject)) {
            return;
        }

        editObject(prismObject);

        @NotNull List<ItemPath> itemsPaths = getExcludeItemsPaths();
        removeItemPathsIfPresent(itemsPaths, prismObject);

        String xml = serializer.serialize(object.asPrismObject());
        writer.write(xml);
    }

    protected boolean shouldSkipObject(PrismObject<? extends ObjectType> prismObject) {
        return false;
    }

    // Allows to customize / hardcode object before exporting
    protected void editObject(PrismObject<? extends ObjectType> prismObject) {
        // INtentionally Noop for normal export
    }

    @Override
    protected String getEpilog() {
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }

    protected @NotNull List<ItemPath> getExcludeItemsPaths() {
        return options.getExcludeItems();
    }

    private static void removeItemPathsIfPresent(
            @NotNull List<ItemPath> itemsToSkip,
            PrismObject<? extends ObjectType> prismObject) throws SchemaException {
        if (!itemsToSkip.isEmpty()) {
            prismObject.getValue().removePaths(itemsToSkip);
        }
    }

}
