/*
 * Copyright (c) 2010-2013 Evolveum
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

                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(queryStr);
                    }
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
