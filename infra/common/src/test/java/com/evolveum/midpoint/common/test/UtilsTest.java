/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
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
import java.nio.charset.Charset;

/**
 * Unit tests for Util class
 * 
 * @author semancik
 */
public class UtilsTest {

    private static String FILENAME_BAD_UTF = "src/test/resources/bad-utf.txt";

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

            badStr = Charset.forName("UTF-8").decode(bb).toString();
        }
        finally {
            stream.close();
        }

        System.out.println("Bad: "+badStr);

        String goodStr = Utils.cleanupUtf(badStr);

        System.out.println("Good: "+goodStr);
    }
}
