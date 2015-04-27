/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.ninja;

import com.evolveum.midpoint.cli.ninja.command.Command;
import com.evolveum.midpoint.cli.common.DefaultCommand;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AbstractNinjaTest {

    public static final List<String> conParams = new ArrayList<>();

    public static final String TEST_URL = "http://demo.evolveum.com/midpoint/ws/model-3";
    public static final String TEST_USER = "administrator";
    public static final String TEST_PASSWORD = "5ecr3t";

    static {
        conParams.add(Command.P_URL);
        conParams.add(TEST_URL);
        conParams.add(Command.P_USERNAME);
        conParams.add(TEST_USER);
        conParams.add(Command.P_PASSWORD);
        conParams.add(TEST_PASSWORD);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNinjaTest.class);

    private String command;

    public AbstractNinjaTest(String command) {
        Validate.notNull(command, "Command must not be null");
        this.command = command;
    }

    @BeforeMethod
    public void beforeMethod(Method method) {
        LOG.info("==== " + method.getName() + " ====");
    }

    @AfterMethod
    public void afterMethod() {
        LOG.info("====================================================");
    }

    @Test
    public void test001Help() {
        Main.main(new String[]{command, Command.P_HELP});
    }

    @Test
    public void test002Version() {
        Main.main(new String[]{DefaultCommand.P_VERSION});
    }

    protected String[] createArgs(String command, String... args) {
        List<String> list = new ArrayList<>();
        list.add(command);

        for (String arg : args) {
            list.add(arg);
        }
        list.addAll(conParams);

        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }
}
