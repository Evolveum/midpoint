/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.perf;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generates data for measuring DB-to-midPoint import performance. See MID-5368.
 *
 * Format is like this:
 *
 * INSERT INTO `people` VALUES('fzum_00000000', 'Gfzum_00000000', 'Sfzum_00000000', 'fzum_00000000@test.edu', '9981283310',
 *      '2019-05-27 14:40:29', NULL, 'CIOXP', 0, 'DXMQW', NULL, 'ShjXu@test.edu',
 *      'DXMQW|VCUKx|EeNkQ|hdRPK|hQcSa|hGDBQ|FdmHk|mYpIt|fdiiT', 'ITPfv', 'f_ReAUQfIKHI', 0, NULL, '2019-05-27');
 *
 */
public class Generate {

    private static final int USERS = 50000;

    public static void main(String[] args) throws IOException {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        PrintWriter pw = new PrintWriter(new FileWriter("target/source-data.sql"));

        for (int i = 0; i < USERS; i++) {
            String login = String.format("%s_%08d", RandomStringUtils.randomAlphabetic(4), i);
            String employeeNumber = RandomStringUtils.randomNumeric(10);
            String activation = dateTimeFormat.format(new Date());
            String department = RandomStringUtils.randomAlphabetic(5);
            List<String> roles = generateValues(10);
            List<String> services = generateValues(3);
            List<String> aliases = generateValues(3);
            String facultyCode = "f_" + RandomStringUtils.randomAlphabetic(10);

            pw.print(String.format("INSERT INTO `people` VALUES('%s', '%s', '%s', '%s@test.edu', '%s', ",
                    login, "G"+login, "S"+login, login, employeeNumber));
            pw.print(String.format("'%s', NULL, '%s', %d, '%s', NULL, '%s', '%s', '%s', '%s', ", activation, department, i,
                    roles.get(0), concat(aliases, "@test.edu"), concat(roles, ""), concat(services, ""), facultyCode));
            pw.println(String.format("%d, NULL, '%s');", i, dateFormat.format(new Date())));
        }

        pw.close();
    }

    private static String concat(List<String> strings, String suffix) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                sb.append("|");
            }
            sb.append(string).append(suffix);
        }
        return sb.toString();
    }

    @NotNull
    private static List<String> generateValues(int limit) {
        List<String> rv = new ArrayList<>();
        int r = (int) (Math.random() * limit) + 1;
        while (r-- > 0) {
            rv.add(RandomStringUtils.randomAlphabetic(5));
        }
        return rv;
    }
}
