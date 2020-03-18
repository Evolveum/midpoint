/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sql.util.RUtil;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class RUtilTest extends BaseSQLRepoTest {

    private static final String USER_BIG = "user-big.xml";

    @Test
    public void test100XmlToByteArrayCompressionEnabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)), StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, true);
        logger.info("Compression ratio: {}", getCompressRatio(xml.getBytes(StandardCharsets.UTF_8.name()).length, array.length));

        String xmlNew = RUtil.getXmlFromByteArray(array, true);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    @Test
    public void test200XmlToByteArrayCompressionDisabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)), StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, false);
        logger.info("Compression ratio: {}", getCompressRatio(xml.getBytes(StandardCharsets.UTF_8.name()).length, array.length));

        AssertJUnit.assertEquals(xml.getBytes(StandardCharsets.UTF_8.name()), array);

        String xmlNew = RUtil.getXmlFromByteArray(array, false);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    @Test
    public void test250ByteArrayToXmlShouldBeCompressed() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)), StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, false);

        String xmlNew = RUtil.getXmlFromByteArray(array, true);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    public double getCompressRatio(double xmlSize, double byteSize) {
        return 100 - (byteSize * 100 / xmlSize);
    }
}
