package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.jetbrains.annotations.NotNull;


import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import org.testng.annotations.Test;

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
        String inputStream = getCachedStream(Paths.get("common", format , ns ,"user-jack." + format));
        Stopwatch timer = stopwatch("parse." + format + "." + ns);
        @NotNull
        PrismObject<Objectable> result = null;
        for(int i = 1; i <=REPETITIONS;i++) {
            @NotNull
            PrismParser parser = PrismTestUtil.getPrismContext().parserFor(inputStream);
            try(Split parsingSplit = timer.start()) {
               result  = parser.parse();
            };
        }
        assertNotNull(result);
    }

    void textParsing() {

    }

    private String getCachedStream(Path path) throws IOException {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();
        CharStreams.copy(MoreFiles.asCharSource(Paths.get("src","test","resources").resolve(path), Charsets.UTF_8).openStream(), builder);
        return builder.toString();
    }



}
