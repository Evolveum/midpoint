/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.security;

import java.util.ArrayList;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for privacy-enhancing setup. E.g. broad get authorizations, but limited search.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class PerfTestRoleMembers extends TestRoleMembers {

    private static final int REPETITIONS = 5000;


    @DataProvider(name = "repetitions")
    public Object[][] repetitions() {
        ArrayList<Object[]> configs = new ArrayList<>();
        for (int rep  = 0; rep < REPETITIONS; rep++) {
            configs.add(new Object[] { rep });
        }
        return configs.toArray(new Object[configs.size()][]);
    }

    /**
     * MID-4893, MID-4947
     */
    @Test(dataProvider =  "repetitions")
    public void test100AutzGuybrushNoMembers(int repetition) throws Exception {
        test100AutzGuybrushNoMembers();
    }

    /**
     * MID-4893
     */
    @Test(dataProvider =  "repetitions")
    public void test105AutzElaineMembers(int repetition) throws Exception {
        test105AutzElaineMembers();
    }
}
