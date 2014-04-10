package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.io.IOUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RUtilTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(RUtilTest.class);

    @Test
    public void test100XmlToByteArrayCompressionEnabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, "user-big.xml")), "utf-8");

        byte[] array = RUtil.getByteArrayFromXml(xml, true);
        LOGGER.info("Compression ratio: {}", getCompressRatio(xml.getBytes("utf-8").length, array.length));

        String xmlNew = RUtil.getXmlFromByteArray(array, true);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    @Test
    public void test200XmlToByteArrayCompressionDisabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, "user-big.xml")), "utf-8");

        byte[] array = RUtil.getByteArrayFromXml(xml, false);
        LOGGER.info("Compression ratio: {}", getCompressRatio(xml.getBytes("utf-8").length, array.length));

        AssertJUnit.assertEquals(xml.getBytes("utf-8"), array);

        String xmlNew = RUtil.getXmlFromByteArray(array, false);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    public double getCompressRatio(double xmlSize, double byteSize) {
        return 100 - (byteSize * 100 / xmlSize);
    }
}