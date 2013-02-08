/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.ninja;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lazyman
 */
public class ImportDDL {

    private String driver;
    private String url;
    private String username;
    private String password;

    private String filePath;

    public ImportDDL(String filePath, String driver, String url, String username, String password) {
        this.filePath = filePath;

        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public boolean execute() {
        File script = new File(filePath);
        if (!script.exists() || !script.canRead()) {
            System.out.println("DDL script file '" + filePath + "' doesn't exist or can't be read.");
            return false;
        }

        Connection connection = null;
        BufferedReader reader = null;
        try {
            connection = createConnection(driver, url, username, password);

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(script), "utf-8"));
            StringBuilder query = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                //skip comments
                if (line.length() == 0 || line.length() > 0 && line.charAt(0) == '-') {
                    continue;
                }

                query.append(' ').append(line);


                //If one command complete
                if (query.charAt(query.length() - 1) == ';') {
                    query.deleteCharAt(query.length() - 1);

                    try {
                        Statement stmt = connection.createStatement();
                        stmt.execute(query.toString());
                        stmt.close();
                    } catch (SQLException ex) {
                        System.out.println("Exception occurred during SQL statement '" + query.toString()
                                + "' execute, reason: " + ex.getMessage());
                    }
                    query = new StringBuilder();
                }
            }

        } catch (Exception ex) {
            System.out.println("Exception occurred, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);

            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (Exception ex) {
                System.out.println("Couldn't close JDBC connection.");
            }
        }

        return true;
    }

    private Connection createConnection(String driver, String url, String username, String password) {
        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, username, password);
        } catch (Exception ex) {
            String pwd = password == null ? "<null>" : StringUtils.repeat("*", password.length());
            System.out.print("Couldn't create JDBC connection to '" + url + "' with username '" + username
                    + "' and password '" + pwd + "', reason: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }
}
