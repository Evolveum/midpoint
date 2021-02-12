/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.tools.testng.PerformanceTestClassMixin;
import com.evolveum.midpoint.util.exception.SchemaException;

public class PerfTestCodecObject extends AbstractSchemaTest implements PerformanceTestClassMixin {

    static final List<String> FORMAT = ImmutableList.of("xml", "json", "yaml");
    static final List<String> NS = ImmutableList.of("no-ns", "ns");

    static final int REPETITIONS = 1000;

    @Test
    void testAll() throws SchemaException, IOException {
        for (String format : FORMAT) {
            for (String ns : NS) {
                testCombination(format, ns);
            }
        }
    }

    void testCombination(String format, String ns) throws SchemaException, IOException {
        String inputStream = getCachedStream(Paths.get("common", format, ns, "user-jack." + format));

        Stopwatch parseTimer = stopwatch(monitorName("parse", format, ns),
                "Parsing user as " + format + " with " + ns);
        Stopwatch serializeTimer = stopwatch(monitorName("serialize", format, ns),
                "Serializing user as " + format + " with " + ns);

        for (int i = 1; i <= REPETITIONS; i++) {
            PrismObject<Objectable> result;
            try (Split ignored = parseTimer.start()) {
                PrismParser parser = PrismTestUtil.getPrismContext().parserFor(inputStream);
                result = parser.parse();
            }
            assertNotNull(result);

            PrismSerializer<String> serializer = PrismTestUtil.getPrismContext().serializerFor(format);
            String serialized;
            try (Split ignored = serializeTimer.start() ) {
                serialized = serializer.serialize(result);
            }
            assertNotNull(serialized);
            assertTrue(!serialized.isEmpty());
        }
    }

    private String getCachedStream(Path path) throws IOException {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();
        CharStreams.copy(MoreFiles.asCharSource(Paths.get("src", "test", "resources").resolve(path), Charsets.UTF_8).openStream(), builder);
        return builder.toString();
    }
}
