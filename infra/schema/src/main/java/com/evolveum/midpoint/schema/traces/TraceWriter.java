/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.evolveum.midpoint.prism.SerializationOptions;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

@Experimental
public class TraceWriter {

    private static final String ZIP_ENTRY_NAME = "trace.xml";

    @NotNull private final PrismContext prismContext;

    public TraceWriter(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @NotNull
    public String writeTrace(TracingOutputType tracingOutput, File file, boolean zip) throws SchemaException, IOException {
        String xml = prismContext.xmlSerializer()
                .options(
                        SerializationOptions
                                .createSerializeReferenceNames()
                                .escapeInvalidCharacters(true)
                                .serializeUnsupportedTypesAsString(true))
                .serializeRealValue(tracingOutput);
        if (zip) {
            writeZipFile(file, ZIP_ENTRY_NAME, xml, StandardCharsets.UTF_8);
        } else {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.write(xml);
            }
        }
        return xml;
    }

    private static void writeZipFile(File file, String entryName, String content, Charset charset) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            try (Writer writer = new OutputStreamWriter(zipOut, charset)) {
                writer.write(content);
            }
        }
    }
}
