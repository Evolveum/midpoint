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

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lazyman
 */
public class ImportDDL {

    private ImportDDLConfig config;

    public ImportDDL(ImportDDLConfig config) {
        this.config = config;
    }

    public boolean execute() {
        System.out.println("Starting DDL import.");

        File script = new File(config.getFilePath());
        if (!script.exists() || !script.canRead()) {
            System.out.println("DDL script file '" + script.getAbsolutePath() + "' doesn't exist or can't be read.");
            return false;
        }

        Connection connection = null;
        BufferedReader reader = null;
        try {
            connection = createConnection();
            if (connection == null) {
                return false;
            }

            readScript(script, reader, connection);
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
                System.out.println("Couldn't close JDBC connection, reason: " + ex.getMessage());
            }
        }

        System.out.println("DDL import finished.");
        return true;
    }

    private void readScript(File script, BufferedReader reader, Connection connection) throws IOException {
        System.out.println("Reading DDL script file '" + script.getAbsolutePath() + "'.");
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(script), "utf-8"));
        StringBuilder query = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            //skip comments
            if (line.length() == 0 || line.length() > 0 && line.charAt(0) == '-') {
                continue;
            }

            if (query.length() != 0) {
                query.append(' ');
            }
            query.append(line.trim());

            //If one command complete
            if (query.charAt(query.length() - 1) == ';') {
                query.deleteCharAt(query.length() - 1);
                try {
                    String queryStr = query.toString();
                    System.out.println("Executing query: " + queryStr);

                    Statement stmt = connection.createStatement();
                    stmt.execute(queryStr);
                    stmt.close();
                } catch (SQLException ex) {
                    System.out.println("Exception occurred during SQL statement '" + query.toString()
                            + "' execute, reason: " + ex.getMessage());
                }
                query = new StringBuilder();
            }
        }
    }

    private Connection createConnection() {
        System.out.println("Creating JDBC connection.");

        String password = !config.isPromptForPassword() ? config.getPassword() : promptForPassword();
        try {
            Class.forName(config.getDriver());
            return DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
        } catch (Exception ex) {
            String pwd = password == null ? "<null>" : StringUtils.repeat("*", password.length());
            System.out.println("Couldn't create JDBC connection to '" + config.getUrl() + "' with username '"
                    + config.getUsername() + "' and password '" + pwd + "', reason: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    private String promptForPassword() {
        String password = null;
        BufferedReader reader = null;
        try {
            System.out.print("Password: ");
            reader = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
            password = reader.readLine();
        } catch (Exception ex) {
            System.out.println("Exception occurred during password prompt, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return password;
    }
}
