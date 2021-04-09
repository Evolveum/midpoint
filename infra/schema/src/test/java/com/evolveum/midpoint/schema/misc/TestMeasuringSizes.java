/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.misc;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Measures sizes of selected object parts.
 *
 * Run on demand. Not present in the test suite. (Actually, this is probably not a test at all.)
 */
@Experimental
public class TestMeasuringSizes extends AbstractSchemaTest {

    private static final File TEST_DIR = new File("d:\\evo\\buckets\\");
    private static final File OBJECTS_TO_MEASURE = new File(TEST_DIR, "objects-to-measure.xml");
    private static final File REPORT_FILE = new File(TEST_DIR, "report.csv");

    private static final List<String> TAGS = Arrays.asList("workState", "operationStats", "diagnosticInformation");

    @Test
    public void testMeasuringSizes() throws SchemaException, IOException {
        PrismContext prismContext = getPrismContext();

        List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(OBJECTS_TO_MEASURE).parseObjects();
        PrintWriter pw = new PrintWriter(new FileWriter(REPORT_FILE));
        writeHeader(pw);
        for (PrismObject<? extends Objectable> object : objects) {
            measure(object, pw);
        }
        pw.close();
    }

    private void writeHeader(PrintWriter pw) {
        pw.print("\"OID\";\"name\"");
        TAGS.forEach(tag -> pw.append(";\"").append(tag).append("\""));
        pw.println(";\"other\";\"total\"");
    }

    private void measure(PrismObject<?> object, PrintWriter pw) throws SchemaException {
        PrismContext prismContext = getPrismContext();

        pw.append("\"").append(object.getOid()).append("\"");
        pw.append(";").append(StringEscapeUtils.escapeCsv(PolyString.getOrig(object.asObjectable().getName())));

        String serialized = prismContext.xmlSerializer().serialize(object);
        int totalLength = serialized.length();
        int tagsLength = 0;
        for (String tag : TAGS) {
            int tagLength = measureTagLength(serialized, tag);
            pw.append(";\"").append(String.valueOf(tagLength)).append("\"");
            tagsLength += tagLength;
        }
        pw.append(";\"").append(String.valueOf(totalLength - tagsLength)).append("\"");
        pw.append(";\"").append(String.valueOf(totalLength)).append("\"\n");
    }

    /**
     * TODO make more robust
     */
    private int measureTagLength(String serialized, String tag) {
        int begin = serialized.indexOf("<" + tag + ">");
        int end = serialized.lastIndexOf("</" + tag + ">");
        if (begin >= 0 && end >= 0) {
            return end - begin;
        } else {
            return 0;
        }
    }
}
