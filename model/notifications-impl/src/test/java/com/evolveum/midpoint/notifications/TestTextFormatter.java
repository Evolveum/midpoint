package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.notifications.formatters.TextFormatter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-provisioning.xml",
        "classpath*:ctx-repository.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-model.xml",
        "classpath:ctx-notifications-test.xml",
        "classpath*:ctx-notifications.xml"})
public class TestTextFormatter extends AbstractTestNGSpringContextTests {

    public static final String OBJECTS_DIR_NAME = "src/test/resources/objects";
    public static final String USER_JACK_FILE = OBJECTS_DIR_NAME + "/user-jack.xml";

    private TextFormatter textFormatter;

    @Test
    public void test010() throws Exception {

        // GIVEN

        PrismObject<UserType> jack = PrismTestUtil.parseObject(new File(USER_JACK_FILE));
        System.out.println(jack.dump());
        // WHEN

        String jackFormatted = textFormatter.formatObject(jack, null, false);
        System.out.println("no hidden paths + hide operational attributes: " + jackFormatted);

        // THEN


    }
}
