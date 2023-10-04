/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class NinjaNativeTest extends NinjaSpringTest {

    private static final Trace LOGGER = TraceManager.getTrace(NinjaNativeTest.class);

    public static final File RESOURCES_DIR = new File("./src/test/resources");

    public static final String NINJA_NATIVE_DB = "ninja_native_tests";
    public static final String NINJA_NATIVE_DB_USERNAME = NINJA_NATIVE_DB;
    public static final String NINJA_NATIVE_DB_PASSWORD = NINJA_NATIVE_DB;

    public static final File NINJA_NATIVE_INSTALLATION = new File("./target/ninja-native");

    public static final File SCRIPT_CREATE_DB = new File(RESOURCES_DIR, "ninja-native/create-ninja-native-db.sql");
    public static final File SCRIPT_DROP_DB = new File(RESOURCES_DIR, "ninja-native/drop-ninja-native-db.sql");
    public static final File CONFIG_XML_TEMPLATE = new File(RESOURCES_DIR, "ninja-native/config-template.xml");

    public static final String CONFIG_XML_JDBC_URL_PLACEHOLDER = "NINJA_NATIVE_JDBC_URL_PLACEHOLDER";

    @Autowired
    private DataSource dataSource;

    private String jdbcUrlNinjaNativeDb;

    private File ninjaMidpointHome;

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        if (!repository.isNative()) {
            throw new SkipException("Skipping tests because repository is not using native PostgreSQL implementation.");
        }

        setupNinjaNativeInstallation();
    }

    @AfterClass
    public void afterClass() throws Exception {
        dropIfExistsNinjaNativeTestsDatabase();
    }

    private void setupNinjaNativeInstallation() throws Exception {
        String jdbcUrl = repository.getRepositoryDiag().getRepositoryUrl().replaceFirst("jdbc:", "");
        URI uri = new URI(jdbcUrl);
        jdbcUrlNinjaNativeDb = "jdbc:" + UriBuilder.fromUri(uri).replacePath(NINJA_NATIVE_DB).toTemplate();

        Assertions.assertThat(jdbcUrlNinjaNativeDb).withFailMessage("JDBC url for ninja native db is not defined").isNotEmpty();

        dropIfExistsNinjaNativeTestsDatabase();

        createNinjaNativeTestsDatabase();

        setupInstallationFolders();
    }

    private void setupInstallationFolders() throws IOException {
        if (NINJA_NATIVE_INSTALLATION.exists()) {
            FileUtils.deleteDirectory(NINJA_NATIVE_INSTALLATION);
        }

        FileUtils.forceMkdir(NINJA_NATIVE_INSTALLATION);

        // copy doc, mainly SQL scripts
        File sqlTargetDir = new File(NINJA_NATIVE_INSTALLATION, "doc/config/sql");
        FileUtils.forceMkdir(sqlTargetDir);
        FileUtils.copyDirectory(new File("../../config/sql"), sqlTargetDir);

        // setup var folder (midpoint-home)
        ninjaMidpointHome = new File(NINJA_NATIVE_INSTALLATION, "var");
        FileUtils.forceMkdir(ninjaMidpointHome);

        // setup config.xml
        String configXml = FileUtils.readFileToString(CONFIG_XML_TEMPLATE, StandardCharsets.UTF_8);
        // replace jdbc url placeholder
        configXml = configXml.replace(CONFIG_XML_JDBC_URL_PLACEHOLDER, jdbcUrlNinjaNativeDb);

        File config = new File(ninjaMidpointHome, "config.xml");
        FileUtils.write(config, configXml, StandardCharsets.UTF_8);
    }

    private void createNinjaNativeTestsDatabase() throws Exception {
        executeSqlScript(dataSource, SCRIPT_CREATE_DB);

        SingleConnectionDataSource ninjaNativeDataSource = null;
        try {
            ninjaNativeDataSource = new SingleConnectionDataSource(jdbcUrlNinjaNativeDb, NINJA_NATIVE_DB_USERNAME, NINJA_NATIVE_DB_PASSWORD, true);

            executeSqlScript(ninjaNativeDataSource, new File("../../config/sql/native/postgres.sql"));
            executeSqlScript(ninjaNativeDataSource, new File("../../config/sql/native/postgres-quartz.sql"));
            executeSqlScript(ninjaNativeDataSource, new File("../../config/sql/native/postgres-audit.sql"));
        } finally {
            if (ninjaNativeDataSource != null) {
                ninjaNativeDataSource.destroy();
            }
        }
    }

    private void dropIfExistsNinjaNativeTestsDatabase() throws Exception {
        executeSqlScript(dataSource, SCRIPT_DROP_DB);
    }

    private void executeSqlScript(DataSource dataSource, File... scripts) throws IOException, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autocommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            try {
                for (File script : scripts) {
                    LOGGER.info("Executing script {}", script.getPath());

                    String sql = FileUtils.readFileToString(script, StandardCharsets.UTF_8);

                    Statement stmt = connection.createStatement();
                    stmt.execute(sql);
                    stmt.close();
                }

                LOGGER.info("Scripts executed successfully.");
            } finally {
                connection.setAutoCommit(autocommit);
            }
        }
    }

    @Test
    public void test100Sample() throws Exception {
        System.out.println("");
    }
}
