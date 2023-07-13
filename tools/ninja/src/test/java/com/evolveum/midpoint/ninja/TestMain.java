/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * TODO remove
 * Created by Viliam Repan (lazyman).
 */
@Deprecated
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

//        input = "-v -m ./target/midpoint-home-upgrade setup-database --scripts-directory ../../config/sql/native-new --audit-only --no-audit".split(" ");

//        input = "-v run-sql --jdbc-url jdbc:postgresql://localhost:5432/mid8842_48 --jdbc-username postgres --jdbc-password postgres".split(" ");
//        input = "-v -m ./target/midpoint-home-upgrade run-sql".split(" ");
//        input = "-v -m ./target/midpoint-home-upgrade run-sql --mode repository".split(" ");
//        input = "-v -m ./target/midpoint-home-upgrade run-sql --mode audit".split(" ");

        input = ("-v run-sql "
                + "--jdbc-url jdbc:postgresql://localhost:5432/mid8842_48 "
                + "--jdbc-username postgres "
                + "--jdbc-password postgres "
                + "--scripts-directory . "
                + "--scripts ./src/test/resources/upgrade/midpoint-home/select-query.sql "
                + "--result"
        ).split(" ");

//        input = ("-v run-sql "
//                + "--jdbc-url jdbc:postgresql://localhost:5432/postgres "
//                + "--jdbc-username postgres "
//                + "--jdbc-password postgres "
//                + "--scripts-directory <MAIN_SQL_DIRECTORY> "
//                + "--scripts <CREATE_DATABASE_SCRIPT>"
//        ).split(" ");
//
//        input = ("-v run-sql "
//                + "--jdbc-url jdbc:postgresql://localhost:5432/<NOVA_DB> "
//                + "--jdbc-username <NOVY_PAJAC> "
//                + "--jdbc-password <PAJACOVE_HESLO> "
//                + "--scripts-directory <CESTA_DO_docs/config/sql/native-new> "
//                + "--create"
//        ).split(" ");
//
//        input = ("-v -m <MIDPOINT_HOME_PATH> run-sql "
//                + "--upgrade "
//                + "--mode repository"
//        ).split(" ");

//        input = "-h run-sql".split(" ");

//        System.out.println("Starting process");
//        new ProcessBuilder(
//                "../../_mess/mid8842/.upgrade-process/1685390031006-midpoint-latest-dist/bin/ninja.sh -v --offline -h".split(" ")
//        ).inheritIO().start();
//        System.out.println("Finished main");

        String cmd = null;

//        String cmd = "-m non_existing_folder -v run-sql "
//                + "--jdbc-url jdbc:postgresql://localhost:5432/postgres "
//                + "--jdbc-username postgres "
//                + "--jdbc-password postgres "
//                + "--scripts ./src/test/resources/upgrade/midpoint-home/create-database.sql "
//                + "--result";
//        execute(cmd);
//
//        cmd = "-v run-sql "
//                + "--jdbc-url jdbc:postgresql://localhost:5432/asdf1 "
//                + "--jdbc-username asdf1 "
//                + "--jdbc-password asdf1 "
//                + "--scripts "
//                + "../../config/sql/native-new/postgres-new.sql "
//                + "../../config/sql/native-new/postgres-new-quartz.sql "
//                + "../../config/sql/native-new/postgres-new-audit.sql ";
////                + "--create"; // can't use, we're not in default ninja folder
//        execute(cmd);

//        cmd = "-v run-sql "
//                + "--jdbc-url jdbc:postgresql://localhost:5432/asdf1 "
//                + "--jdbc-username asdf1 "
//                + "--jdbc-password asdf1 "
//                + "--scripts ./src/test/resources/upgrade/midpoint-home/select-query.sql "
//                + "--result";

        cmd = "-h run-sql";
        execute(cmd);
    }

    private static void execute(String args) {
        long time = System.currentTimeMillis();
        System.out.println("STARTING WITH OPTIONS: " + args);
        Main.main(args.split(" "));
        System.out.println("RUN TIME: " + (System.currentTimeMillis() - time));
        System.out.println();
    }

    public void testJANSI() throws Exception {
        AnsiConsole.systemInstall();

//        System.out.print(Ansi.ansi().a("vilko\n"));
//        System.out.print(Ansi.ansi().cursorUpLine().eraseLine());
//        System.out.print(Ansi.ansi().a("janko\n"));

        System.out.println(Ansi.ansi().fgBlue().a("Start").reset());
        for (int i = 0; i < 10; i++) {
            System.out.println(Ansi.ansi().cursorUpLine().eraseLine(Ansi.Erase.ALL).fgGreen().a(i).reset());
            Thread.sleep(500);
        }
        System.out.println(Ansi.ansi().fgRed().a("Complete").reset());

        AnsiConsole.systemUninstall();
    }
}
