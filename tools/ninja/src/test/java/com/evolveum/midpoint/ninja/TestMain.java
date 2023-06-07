/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestMain {

    public static void main(String[] args) {
        //        jc.parse("-m", "./src/test/resources/midpoint-home",
//                "export",
//                "-O", "./export.xml",
//                "-t", "users",
//                "-o", SystemObjectsType.USER_ADMINISTRATOR.value());
        String[] input = new String[] { "-v", "-m", "./src/test/resources/midpoint-home",
                "export",
                "-O", "./export.xml",
                "-t", "roles" };
//                "-f", "<inOid xmlns=\"http://prism.evolveum.com/xml/ns/public/query-3\"><value>00000000-0000-0000-0000-000000000002</value></inOid>");

//        input = "-m ../../_mess/midpoint-home verify --create-report".split(" ");
//        input = "-h upgrade".split(" ");
//        input = ("-m ../../_mess/mid8842/var "
//                + "upgrade "
//                + "--distribution-archive ../../_mess/mid8842/1685390031006-midpoint-latest-dist.zip "
//                + "--installation-directory ../../_mess/mid8842 "
//                + "--backup-midpoint-directory "
//                + "--temp-dir ../../_mess/mid8842/.upgrade-process")
//                .split(" ");

                input = "-h setup-database".split(" ");

//        input = "-v -m ../../_mess/create0 setup-database --scripts-directory ../../config/sql/native-new".split(" ");

        Main.main(input);
    }
}
