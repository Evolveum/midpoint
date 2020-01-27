/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.test;

import org.testng.annotations.Test;
import com.evolveum.midpoint.common.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for Util class
 *
 * @author semancik
 */
public class UtilsTest {

    private static final String FILENAME_BAD_UTF = "src/test/resources/bad-utf.txt";

    public UtilsTest() {
    }

    /**
     * Testing the ability to remove non-UTF characters from string.
     * The test file contains such characters.
     */
    @Test
    public void cleaupUtfTest() throws FileNotFoundException, IOException {

        String badStr;

        // The file contains strange chanrs (no-break spaces), so we need to pull
        // it in exactly as it is.
        File file = new File(FILENAME_BAD_UTF);
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            badStr = StandardCharsets.UTF_8.decode(bb).toString();
        }
        finally {
            stream.close();
        }

        System.out.println("Bad: "+badStr);

        String goodStr = Utils.cleanupUtf(badStr);

        System.out.println("Good: "+goodStr);
    }
}
