/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

@Experimental
public class TraceWriter {

    private static final Trace LOGGER = TraceManager.getTrace(TraceWriter.class);

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
            try {
                MiscUtil.writeZipFile(file, ZIP_ENTRY_NAME, xml, StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("Failed to write to the zip file {}, trying dumping the data", file.getAbsolutePath(), e);
                try {
                    String filename = file.getAbsolutePath() + ".utf16";
                    Files.writeString(Path.of(filename), xml, StandardCharsets.UTF_16LE);
                    LOGGER.info("Wrote the data dump to {}", filename);
                } catch (Exception e1) {
                    LOGGER.error("Failed to encode the data into UTF-16LE for debugging purposes", e1);
                }
                String filename = file.getAbsolutePath() + ".dump.hex";
                try (BufferedWriter w = Files.newBufferedWriter(Path.of(filename))) {
                    for (int i = 0; i < xml.length(); i++) {
                        w.write(String.format("%04X ", (int) xml.charAt(i)));
                    }
                    LOGGER.info("Wrote the hex dump to {}", filename);
                } catch (Exception e2) {
                    LOGGER.error("Failed to write the data to hex dump", e2);
                }
            }
        } else {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.write(xml);
            }
        }
        return xml;
    }
}
