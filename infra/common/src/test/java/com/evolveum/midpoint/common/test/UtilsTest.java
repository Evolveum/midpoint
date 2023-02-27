/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for Util class
 *
 * @author semancik
 */
public class UtilsTest extends AbstractUnitTest {

    private static final String FILENAME_BAD_UTF = "src/test/resources/bad-utf.txt";

    public UtilsTest() {
    }

    /**
     * Testing the ability to remove non-UTF characters from string.
     * The test file contains such characters.
     */
    @Test
    public void cleanupUtfTest() throws IOException {
        String badStr;

        // The file contains strange chars (no-break spaces), so we need to pull
        // it in exactly as it is.
        File file = new File(FILENAME_BAD_UTF);
        try (FileInputStream stream = new FileInputStream(file)) {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            badStr = StandardCharsets.UTF_8.decode(bb).toString();
        }

        System.out.println("Bad: " + badStr);

        String goodStr = Utils.cleanupUtf(badStr);

        System.out.println("Good: " + goodStr);
    }

    @Test
    public void validateObjectOid() {
        assertFalse(Utils.isPrismObjectOidValid(null));
        assertFalse(Utils.isPrismObjectOidValid(""));
        assertFalse(Utils.isPrismObjectOidValid(" "));

        assertTrue(Utils.isPrismObjectOidValid(UUID.randomUUID().toString()));
        assertTrue(Utils.isPrismObjectOidValid(UUID.randomUUID().toString().toUpperCase()));
    }
}
