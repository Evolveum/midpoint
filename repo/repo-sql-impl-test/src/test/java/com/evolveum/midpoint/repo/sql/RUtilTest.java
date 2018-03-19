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
import java.nio.charset.StandardCharsets;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RUtilTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(RUtilTest.class);

    private static final String USER_BIG = "user-big.xml";
    
    @Test
    public void test100XmlToByteArrayCompressionEnabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)), StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, true);
        LOGGER.info("Compression ratio: {}", getCompressRatio(xml.getBytes( StandardCharsets.UTF_8.name()).length, array.length));

        String xmlNew = RUtil.getXmlFromByteArray(array, true);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    @Test
    public void test200XmlToByteArrayCompressionDisabled() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)),  StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, false);
        LOGGER.info("Compression ratio: {}", getCompressRatio(xml.getBytes( StandardCharsets.UTF_8.name()).length, array.length));

        AssertJUnit.assertEquals(xml.getBytes( StandardCharsets.UTF_8.name()), array);

        String xmlNew = RUtil.getXmlFromByteArray(array, false);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    @Test
    public void test250ByteArrayToXmlShouldBeCompressed() throws Exception {
        String xml = IOUtils.toString(new FileInputStream(
                new File(BaseSQLRepoTest.FOLDER_BASIC, USER_BIG)),  StandardCharsets.UTF_8.name());

        byte[] array = RUtil.getByteArrayFromXml(xml, false);

        String xmlNew = RUtil.getXmlFromByteArray(array, true);

        AssertJUnit.assertEquals(xml, xmlNew);
    }

    public double getCompressRatio(double xmlSize, double byteSize) {
        return 100 - (byteSize * 100 / xmlSize);
    }
}