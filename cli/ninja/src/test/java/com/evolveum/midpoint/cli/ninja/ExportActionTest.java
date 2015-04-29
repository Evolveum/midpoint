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

import com.evolveum.midpoint.cli.ninja.command.Export;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ExportActionTest extends AbstractNinjaTest {

    public ExportActionTest() {
        super(Export.CMD_EXPORT);
    }

    @Test
    public void test100ExportObjects() throws Exception {
        String file = createFileName("test100ExportObjects");
        testExport(null, null, file, false);

        //todo asserts
    }

    @Test
    public void test200ExportUsers() throws Exception {
        String file = createFileName("test101ExportUsers");
        testExport("user", null, file, false);

        //todo asserts
    }

    @Test
    public void test300ExportAdministrator() throws Exception {
        String file = createFileName("test102ExportAdministrator");
        testExport("user", SystemObjectsType.USER_ADMINISTRATOR.value(), file, false);

        //todo asserts
    }

    private String createFileName(String name) {
        return "./target/" + name + ".xml";
    }

    private void testExport(String type, String oid, String file, boolean verbose) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(type)) {
            list.add(Export.P_TYPE);
            list.add(type);
        }

        if (StringUtils.isNotEmpty(oid)) {
            list.add(Export.P_OID);
            list.add(oid);
        }

        if (StringUtils.isNotEmpty(file)) {
            list.add(Export.P_FILE);
            list.add(file);
        }

        String[] array = new String[list.size()];
        list.toArray(array);

        String[] args = createArgs(Export.CMD_EXPORT, array);
        Main.main(args);
    }
}
