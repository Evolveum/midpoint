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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story test for importing an existing low-code (manifest-based) connector installed as a
 * <b>JAR bundle</b> into the connector development - the JAR counterpart of
 * {@link ConnectorImportStoryTest} (which uses a directory bundle). Replicates the scenario of
 * bug MID-12617: a connector exported as a .jar and installed into the connector scan directory
 * must be resumable - {@code startFromExisting} reads the manifest and the scripts from the JAR,
 * and the copy-connector step unpacks the JAR into a new (directory) bundle under the bumped
 * version so that it can coexist with the original JAR. The source bundle is the
 * {@code connector-scimrest-generic} Maven artifact, copied into {@code target/dependency}
 * by the build (see {@link ScimRestTemplateJar}). No AI / connector-generation microservice
 * is involved.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConnectorImportFromJarStoryTest extends AbstractEmptyModelIntegrationTest {

    private static final int TIMEOUT = 300_000;

    // The SCIM/REST framework template bundle, obtained from Maven (see ScimRestTemplateJar).
    private static final File TEMPLATE_JAR = ScimRestTemplateJar.get();

    // A distinct bundle name (the JAR's manifest is re-stamped with it when packing), so the
    // story can be run together with ConnectorImportStoryTest, which uses the generic template
    // bundle name - a leftover copy of the same bundle/version would otherwise make the fresh
    // context's bundle scan consider the copy "already installed".
    private static final String BUNDLE_NAME = "com.evolveum.polygon.scimrest.connector-scimrest-jar";
    private static final String GROUP_ID = "com.evolveum.polygon.scimrest";
    private static final String ARTIFACT_ID = "connector-scimrest-jar";
    private static final String SOURCE_VERSION = ScimRestTemplateJar.VERSION;
    // ConnectorVersionUtil.bumpMinor(SOURCE_VERSION)
    private static final String TARGET_VERSION = "0.3.0";

    private static final String MANIFEST_YAML = """
            application:
              name: Imported Test App
              description: A low-code connector imported into the development
              version: 3.1
              apiVersion: v2
            connector:
              integrationType: rest
              authMethods:
                - type: basic
                  name: HTTP Basic Authorization
                - type: apiKey
                  name: API Key
                  quirks: Sent in the X-Api-Key header
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
    private File sourceBundleJar;
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
        sourceBundleJar = packAsJar(sourceBundleDir,
                new File(sourceBundleDir.getParentFile(), ARTIFACT_ID + "-connector-" + SOURCE_VERSION + ".jar"));

        var infos = connectorFactory.addLocalConnector(sourceBundleJar.toURI());
        assertThat(infos).as("ConnId should accept the synthetic low-code bundle JAR").isNotEmpty();
        var key = infos.get(0).getConnectorKey();

        var connector = new ConnectorType()
                .name(BUNDLE_NAME + " " + key.getBundleVersion())
                .displayName(BUNDLE_NAME + " " + key.getBundleVersion())
                .framework(SchemaConstants.ICF_FRAMEWORK_URI)
                .connectorBundle(key.getBundleName())
                .connectorType(key.getConnectorName())
                .connectorVersion(key.getBundleVersion());
        sourceConnectorOid = addObject(connector.asPrismObject(), initTask, initResult);
        logger.info("Registered source connector JAR {} -> {} {}",
                sourceConnectorOid, key.getBundleName(), key.getBundleVersion());
    }

    /**
     * Builds a low-code connector bundle from the framework template: unpacks the template jar
     * (which carries the {@code ManifestBasedConnector} class and dev-mode support) and adds a
     * {@code connector.manifest.yaml} plus the scripts it references.
     */
    private File buildSourceBundle() throws IOException {
        var dir = Files.createTempDirectory("conndev-import-jar-source-").toFile();
        try (var in = new ZipInputStream(new FileInputStream(TEMPLATE_JAR))) {
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

    /**
     * Packs the bundle into a full connector JAR the same way the wizard's export does: the
     * bundle's own {@code META-INF/MANIFEST.MF} becomes the JAR's official manifest (carrying
     * the {@code ConnectorBundle-*} attributes), the manifest itself is not written again as a
     * regular entry. The bundle name/version are re-stamped to the story's own coordinates.
     */
    private File packAsJar(File bundleDir, File targetJar) throws IOException {
        Manifest manifest;
        try (var in = new FileInputStream(new File(bundleDir, "META-INF/MANIFEST.MF"))) {
            manifest = new Manifest(in);
        }
        var mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(new Attributes.Name("ConnectorBundle-Name"), BUNDLE_NAME);
        mainAttributes.put(new Attributes.Name("ConnectorBundle-Version"), SOURCE_VERSION);
        Files.deleteIfExists(targetJar.toPath());
        try (var jar = new JarOutputStream(new FileOutputStream(targetJar), manifest)) {
            var root = bundleDir.toPath();
            try (Stream<Path> files = Files.walk(root)) {
                var regularFiles = files.filter(Files::isRegularFile).sorted().toList();
                for (Path file : regularFiles) {
                    var entryName = root.relativize(file).toString().replace(File.separatorChar, '/');
                    if ("META-INF/MANIFEST.MF".equals(entryName)) {
                        continue;
                    }
                    jar.putNextEntry(new JarEntry(entryName));
                    Files.copy(file, jar);
                    jar.closeEntry();
                }
            }
        }
        return targetJar;
    }

    @Test
    public void test050IsManifestBased() throws CommonException {
        var connector = getObject(ConnectorType.class, sourceConnectorOid).asObjectable();
        assertThat(connector.getConnectorType())
                .as("the JAR declares the low-code framework class").contains("ManifestBasedConnector");
        assertThat(connectorService.isManifestBasedConnector(connector))
                .as("the JAR-based source connector is recognized as low-code").isTrue();
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
        // The manifest declares "rest", while the class-based inference of the scimrest template
        // bundle would yield SCIM - the manifest wins.
        assertThat(connDef.getIntegrationType())
                .as("integration type taken from the manifest, overriding the class-based inference")
                .isEqualTo(ConnDevIntegrationType.REST);
        assertThat(development.getApplication().getIntegrationType())
                .isEqualTo(ConnDevIntegrationType.REST);
        assertThat(development.getApplication().getVersion()).isEqualTo("3.1");
        assertThat(development.getApplication().getApiVersion()).isEqualTo("v2");
        assertThat(connDef.getAuth()).extracting(ConnDevAuthInfoType::getType)
                .as("selected auth methods prefilled from the manifest")
                .containsExactly(ConnDevHttpAuthTypeType.BASIC, ConnDevHttpAuthTypeType.API_KEY);
        assertThat(connDef.getAuth().get(1).getName()).isEqualTo("API Key");
        assertThat(connDef.getAuth().get(1).getQuirks()).isEqualTo("Sent in the X-Api-Key header");
        assertThat(connDef.getSourceConnectorRef()).isNotNull();
        assertThat(connDef.getSourceConnectorRef().getOid()).isEqualTo(sourceConnectorOid);

        var user = connDef.getObjectClass().stream()
                .filter(o -> o.getName().equals("User")).findFirst().orElse(null);
        assertThat(user).as("object class prefilled from the manifest").isNotNull();
        assertThat(user.getNativeSchemaScript()).isNotNull();
        assertThat(user.getNativeSchemaScript().getContent())
                .as("script content read from the source JAR")
                .contains("native schema for User");

        // The application's detected schema is still empty here - the object classes prefilled on
        // the connector item are enriched into it by the discovery steps.
        assertThat(development.getApplication().getDetectedSchema()).isNotNull();
        assertThat(development.getApplication().getDetectedSchema().getObjectClass()).isEmpty();
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

        // The JAR source is unpacked into a new directory bundle installed under the bumped
        // version, coexisting with the original JAR connector.
        var newConnector = getObject(ConnectorType.class, response.getConnectorRef().getOid()).asObjectable();
        assertThat(newConnector.getConnectorBundle()).isEqualTo(BUNDLE_NAME);
        assertThat(newConnector.getConnectorVersion()).isEqualTo(TARGET_VERSION);

        // The original connector is still present under the source version.
        assertThat(getObject(ConnectorType.class, sourceConnectorOid).asObjectable().getConnectorVersion())
                .isEqualTo(SOURCE_VERSION);

        // The copy is a real directory bundle (not the JAR), carrying the recomputed manifest;
        // the source JAR itself is left untouched.
        var copyDir = new File(new File(System.getProperty("midpoint.home"), "connid-connectors"),
                BUNDLE_NAME + "." + TARGET_VERSION);
        assertThat(copyDir).as("the copied bundle is a directory under the scan directory").isDirectory();
        assertThat(new File(copyDir, "connector.manifest.yaml")).exists();
        assertThat(sourceBundleJar).as("the source JAR is not modified or removed").isFile();

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
