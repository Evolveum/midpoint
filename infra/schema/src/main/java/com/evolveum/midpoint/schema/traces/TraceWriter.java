/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.MiscUtil;

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
            MiscUtil.writeZipFile(file, ZIP_ENTRY_NAME, xml, StandardCharsets.UTF_8);
        } else {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.write(xml);
            }
        }
        return xml;
    }
}
