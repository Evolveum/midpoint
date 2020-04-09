/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

@Experimental
public class TraceParser {

    private static final Trace LOGGER = TraceManager.getTrace(TraceParser.class);

    @NotNull private final PrismContext prismContext;

    @SuppressWarnings("WeakerAccess") // used externally
    public TraceParser(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public TracingOutputType parse(File file) throws IOException, SchemaException {
        boolean isZip = file.getName().toLowerCase().endsWith(".zip");
        return parse(new FileInputStream(file), isZip, file.getPath());
    }

    public TracingOutputType parse(InputStream inputStream, boolean isZip, String description) throws SchemaException, IOException {
        TracingOutputType wholeTracingOutput = getObject(inputStream, isZip, description);
        if (wholeTracingOutput != null) {
            new DictionaryExpander(wholeTracingOutput).expand();
            new OperationCategorizer(wholeTracingOutput).categorize();
        }
        return wholeTracingOutput;
    }

    public TracingOutputType getObject(InputStream stream, boolean isZip, String description) throws IOException, SchemaException {
        long start = System.currentTimeMillis();
        Object object;
        if (isZip) {
            try (ZipInputStream zis = new ZipInputStream(stream)) {
                ZipEntry zipEntry = zis.getNextEntry();
                if (zipEntry != null) {
                    object = prismContext.parserFor(zis).xml().compat().parseRealValue();
                } else {
                    LOGGER.error("No zip entry in input file '{}'", description);
                    object = null;
                }
            }
        } else {
            object = prismContext.parserFor(stream).xml().compat().parseRealValue();
        }
        stream.close();
        long read = System.currentTimeMillis();
        LOGGER.info("Read the content of {} in {} milliseconds", description, read - start);

        if (object instanceof TracingOutputType) {
            return (TracingOutputType) object;
        } else if (object instanceof OperationResultType) {
            TracingOutputType rv = new TracingOutputType(prismContext);
            rv.setResult((OperationResultType) object);
            return rv;
        } else {
            LOGGER.error("Wrong object type in input file '{}': {}", description, object);
            return null;
        }
    }
}
