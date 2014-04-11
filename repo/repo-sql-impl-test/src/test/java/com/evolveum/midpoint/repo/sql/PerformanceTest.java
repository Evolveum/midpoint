package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PerformanceTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(PerformanceTest.class);

    /**
     * time: ~9min (parsers branch)
     */
    @Test(enabled = false)
    public void testParsing() throws Exception {
        long time = System.currentTimeMillis();

        int COUNT = 1000;
        for (int i = 0; i < COUNT; i++) {
            List<PrismObject<? extends Objectable>> elements = prismContext.parseObjects(new File(FOLDER_BASIC, "objects.xml"));
            for (PrismObject obj : elements) {
                prismContext.serializeObjectToString(obj, PrismContext.LANG_XML);
            }
        }
        LOGGER.info("xxx>> time: {}", (System.currentTimeMillis() - time));
    }
}