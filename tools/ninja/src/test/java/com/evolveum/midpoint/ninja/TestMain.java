/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.util.NinjaUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.postgresql.Driver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestMain {

    public static void main(String[] args) throws Exception {
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

//        input = "-h setup-database".split(" ");
//
//        input = "-v --offline -m ../../_mess/create0 setup-database --scripts-directory ../../config/sql/native-new".split(" ");

//        input = "-v --offline -m ../../_mess/create0 download-distribution".split(" ");

//        input = "-h".split(" ");

//        input = ("-v --offline -m ../../_mess/upgrade/midpoint/var "
//                + "upgrade-distribution "
//                + "--temp-directory ../../_mess/upgrade/midpoint/.upgrade "
//                + "--distribution-archive ../../_mess/upgrade/midpoint-latest-dist.zip "
//                + "--backup-midpoint-directory"
//               ).split(" ");

//        input = "-v -m ../../_mess/upgrade/midpoint-48/var verify --report ../../_mess/upgrade/midpoint-48/verify-report.csv".split(" ");

        input = "-v -m ./target/midpoint-home-upgrade setup-database --scripts-directory ../../config/sql/native-new --audit-only --no-audit".split(" ");
        long time = System.currentTimeMillis();
        Main.main(input);
        System.out.println("TIME: " + (System.currentTimeMillis() - time));

//        System.out.println("Starting process");
//        new ProcessBuilder(
//                "../../_mess/mid8842/.upgrade-process/1685390031006-midpoint-latest-dist/bin/ninja.sh -v --offline -h".split(" ")
//        ).inheritIO().start();
//        System.out.println("Finished main");
    }
}
