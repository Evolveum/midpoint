/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart.conndev;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorFactoryConnIdImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story test for importing an existing low-code (manifest-based) connector into the connector
 * development: {@code startFromExisting} prefills a development from the connector's manifest
 * (bumping the version by one minor level), and the copy-connector step installs the copied
 * bundle under the new version so that it can coexist with the original connector.
 *
 * <p>The source low-code bundle is built from the SCIM/REST framework template (a
 * {@code ManifestBasedConnector} with development-mode support) plus a generated
 * {@code connector.manifest.yaml}. No AI / connector-generation microservice is involved.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConnectorImportStoryTest extends AbstractEmptyModelIntegrationTest {

    private static final int TIMEOUT = 300_000;

    private static final String TEMPLATE_JAR = System.getProperty("conndev.import.template.jars",
            System.getProperty("user.home") + File.separator
                    + ".m2/repository/com/evolveum/polygon/scimrest/connector-scimrest-generic/0.1-SNAPSHOT/connector-scimrest-generic-0.1-SNAPSHOT.jar");

    private static final String BUNDLE_NAME = "com.evolveum.polygon.scimrest.connector-scimrest-generic";
    private static final String GROUP_ID = "com.evolveum.polygon.scimrest";
    private static final String ARTIFACT_ID = "connector-scimrest-generic";
    private static final String SOURCE_VERSION = "0.1-SNAPSHOT";
    private static final String TARGET_VERSION = "0.2-SNAPSHOT";

    private static final String MANIFEST_YAML = """
            application:
              name: Imported Test App
              description: A low-code connector imported into the development
            connector:
              schema:
                - script: /User.native.schema.groovy
                  objectClass: User
                  operation: schema
                  intent: native
              authorization:
                - script: /authentication.op.groovy
                  intent: auth
              operation:
                - script: /test.op.groovy
                  operation: testConnection
            """;

    @Autowired
    private ConnectorDevelopmentService connectorService;

    @Autowired
    private ConnectorFactoryConnIdImpl connectorFactory;

    private File sourceBundleDir;
    private String sourceConnectorOid;
    private String developmentOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        var systemDelta = deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_SMART_INTEGRATION)
                .replace(new SmartIntegrationConfigurationType()
                        .connectorFrameworkUrl("file://" + TEMPLATE_JAR))
                .<SystemConfigurationType>asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        executeChanges(systemDelta, null, initTask, initResult);

        sourceBundleDir = buildSourceBundle();
        var infos = connectorFactory.addLocalConnector(sourceBundleDir.toURI());
        assertThat(infos).as("ConnId should accept the synthetic low-code bundle").isNotEmpty();
        var key = infos.get(0).getConnectorKey();

        var connector = new ConnectorType()
                .name(BUNDLE_NAME + " " + key.getBundleVersion())
                .displayName(BUNDLE_NAME + " " + key.getBundleVersion())
                .framework(SchemaConstants.ICF_FRAMEWORK_URI)
                .connectorBundle(key.getBundleName())
                .connectorType(key.getConnectorName())
                .connectorVersion(key.getBundleVersion());
        sourceConnectorOid = addObject(connector.asPrismObject(), initTask, initResult);
        logger.info("Registered source connector {} -> {} {}",
                sourceConnectorOid, key.getBundleName(), key.getBundleVersion());
    }

    /**
     * Builds a low-code connector bundle from the framework template: unpacks the template jar
     * (which carries the {@code ManifestBasedConnector} class and dev-mode support) and adds a
     * {@code connector.manifest.yaml} plus the scripts it references.
     */
    private File buildSourceBundle() throws IOException {
        var dir = Files.createTempDirectory("conndev-import-source-").toFile();
        try (var in = new ZipInputStream(new java.io.FileInputStream(TEMPLATE_JAR))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                var target = new File(dir, entry.getName()).getCanonicalFile();
                // guard against zip-slip
                if (!target.toPath().startsWith(dir.getCanonicalFile().toPath())) {
                    throw new IOException("Invalid zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (var out = Files.newOutputStream(target.toPath())) {
                        in.transferTo(out);
                    }
                }
            }
        }
        Files.writeString(new File(dir, "connector.manifest.yaml").toPath(), MANIFEST_YAML);
        writeScript(dir, "User.native.schema.groovy", "// native schema for User\n");
        writeScript(dir, "authentication.op.groovy", "// authentication\n");
        writeScript(dir, "test.op.groovy", "// test connection\n");
        return dir;
    }

    private void writeScript(File bundleDir, String name, String content) throws IOException {
        Files.writeString(new File(bundleDir, name).toPath(), content);
    }

    @Test
    public void test050IsManifestBased() throws CommonException {
        var connector = getObject(ConnectorType.class, sourceConnectorOid).asObjectable();
        assertThat(connectorService.isManifestBasedConnector(connector, getTestOperationResult()))
                .as("the source bundle carries a connector.manifest.yaml").isTrue();
    }

    @Test(dependsOnMethods = "test050IsManifestBased")
    public void test100StartFromExisting() throws CommonException {
        // test050 only asserts; the development is created here
        var connector = getObject(ConnectorType.class, sourceConnectorOid).asObjectable();

        var development = connectorService.startFromExisting(connector, getTestTask(), getTestOperationResult());
        developmentOid = development.getOid();
        display("Created development " + developmentOid, development);

        var connDef = development.getConnector();
        assertThat(connDef.getGroupId()).isEqualTo(GROUP_ID);
        assertThat(connDef.getArtifactId()).isEqualTo(ARTIFACT_ID);
        assertThat(connDef.getVersion()).as("version bumped by one minor level").isEqualTo(TARGET_VERSION);
        assertThat(connDef.getIntegrationType()).as("inferred from the @ConnectorClass package")
                .isEqualTo(ConnDevIntegrationType.SCIM);
        assertThat(connDef.getSourceConnectorRef()).isNotNull();
        assertThat(connDef.getSourceConnectorRef().getOid()).isEqualTo(sourceConnectorOid);

        var user = connDef.getObjectClass().stream()
                .filter(o -> o.getName().equals("User")).findFirst().orElse(null);
        assertThat(user).as("object class prefilled from the manifest").isNotNull();
        assertThat(user.getNativeSchemaScript()).isNotNull();
        assertThat(user.getNativeSchemaScript().getContent()).contains("native schema for User");

        assertThat(development.getApplication().getDetectedSchema().getObjectClass())
                .extracting(ConnDevObjectClassInfoType::getName)
                .contains("User");
    }

    @Test(dependsOnMethods = "test100StartFromExisting")
    public void test200ReusesExistingDevelopment() throws CommonException {
        var connector = getObject(ConnectorType.class, sourceConnectorOid).asObjectable();
        var again = connectorService.startFromExisting(connector, getTestTask(), getTestOperationResult());
        assertThat(again.getOid()).as("reusing the existing development").isEqualTo(developmentOid);
    }

    @Test(dependsOnMethods = "test100StartFromExisting")
    public void test300CopyConnector() throws Exception {
        var operation = continueDevelopment();

        var token = operation.submitCopyConnector(getTestTask(), getTestOperationResult());
        assertThat(token).isNotNull();

        var response = waitForFinish(
                () -> connectorService.getCopyConnectorStatus(token, getTestTask(), getTestOperationResult()),
                TIMEOUT);
        assertThat(response).isNotNull();
        assertThat(response.getConnectorRef()).isNotNull();

        // The copied bundle is installed under the bumped version, coexisting with the source.
        var newConnector = getObject(ConnectorType.class, response.getConnectorRef().getOid()).asObjectable();
        assertThat(newConnector.getConnectorBundle()).isEqualTo(BUNDLE_NAME);
        assertThat(newConnector.getConnectorVersion()).isEqualTo(TARGET_VERSION);

        // The original connector is still present under the source version.
        assertThat(getObject(ConnectorType.class, sourceConnectorOid).asObjectable().getConnectorVersion())
                .isEqualTo(SOURCE_VERSION);

        var reloaded = continueDevelopment().getObject();
        assertThat(reloaded.getConnector().getConnectorRef()).isNotNull();
        assertThat(reloaded.getConnector().getConnectorRef().getOid())
                .isEqualTo(newConnector.getOid());
    }

    private ConnectorDevelopmentOperation continueDevelopment() throws CommonException {
        return connectorService.continueFrom(
                getObject(ConnectorDevelopmentType.class, developmentOid).asObjectable());
    }

    /**
     * Removes a leftover copy of the generated connector bundle from a previous run. The ConnId
     * factory scans the connector download directory when the context is created and remembers
     * every bundle it finds, so a stale copy of the same bundle/version would make the
     * copy-and-install step of this test report "connector already installed". The download
     * directory lives under {@code ${midpoint.home}} (a per-build directory), hence this runs
     * before the Spring context is created.
     */
    @BeforeSuite
    public static void cleanLeftoverConnectorBundle() {
        var home = System.getProperty("midpoint.home");
        if (home == null) {
            return;
        }
        deleteRecursively(new File(new File(home, "connid-connectors"), BUNDLE_NAME + "." + TARGET_VERSION));
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        try (var paths = Files.walk(file.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Couldn't delete " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't delete " + file, e);
        }
    }
}
