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

        input = "-v --offline -m ../../_mess/create0 setup-database --scripts-directory ../../config/sql/native-new".split(" ");

        Main.main(input);

//        HikariConfig config = new HikariConfig();
//        config.setAutoCommit(true);
//        config.setDriverClassName(Driver.class.getName());
//        config.setJdbcUrl("jdbc:postgresql://localhost:5432/create1");
//        config.setUsername("create1");
//        config.setPassword("create1");
//        config.setMaximumPoolSize(5);
//        config.setMinimumIdle(1);
//        HikariDataSource ds = new HikariDataSource(config);
//
//        try {
//            Statement stmt = ds.getConnection().createStatement();
//            stmt.execute(FileUtils.readFileToString(new File("/Users/lazyman/Work/monoted/git/evolveum/midpoint-support/config/sql/native-new/postgres-new.sql"), StandardCharsets.UTF_8));
//
////            NinjaUtils.executeSqlScripts(ds, List.of(
////                    new File("/Users/lazyman/Work/monoted/git/evolveum/midpoint-support/config/sql/native-new/postgres-new.sql"),
////                    new File("/Users/lazyman/Work/monoted/git/evolveum/midpoint-support/config/sql/native-new/postgres-new-quartz.sql"),
////                    new File("/Users/lazyman/Work/monoted/git/evolveum/midpoint-support/config/sql/native-new/postgres-new-audit.sql")
////            ), null);
//        } finally {
//            ds.close();
//        }
    }
}
