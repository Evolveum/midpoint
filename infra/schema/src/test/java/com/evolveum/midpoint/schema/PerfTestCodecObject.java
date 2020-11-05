/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertNotNull;

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
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.util.exception.SchemaException;

public class PerfTestCodecObject extends AbstractSchemaTest implements PerformanceTestMixin {

    static final List<String> FORMAT = ImmutableList.of("xml", "json", "yaml");
    static final List<String> NS = ImmutableList.of("no-ns", "ns");

    static final int REPETITIONS = 20_000;

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

        Stopwatch timer = stopwatch(monitorName("parse", format, ns), "TODO");
        PrismObject<Objectable> result;
        for (int i = 1; i <= REPETITIONS; i++) {
            try (Split ignored = timer.start()) {
                PrismParser parser = PrismTestUtil.getPrismContext().parserFor(inputStream);
                result = parser.parse();
            }
            assertNotNull(result);
        }
    }

    private String getCachedStream(Path path) throws IOException {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();
        CharStreams.copy(MoreFiles.asCharSource(Paths.get("src", "test", "resources").resolve(path), Charsets.UTF_8).openStream(), builder);
        return builder.toString();
    }
}
