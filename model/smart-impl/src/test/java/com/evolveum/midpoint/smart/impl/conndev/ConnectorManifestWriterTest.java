/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevApplicationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevScriptIntentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.xml.sax.SAXException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConnectorManifestWriter} - the canonical manifest dialect it writes,
 * including the application version / API version, the connector integration type and the
 * selected supported auth methods. The output is verified by round-tripping it through
 * {@link ConnectorManifestReader}.
 */
public class ConnectorManifestWriterTest {

    @BeforeClass
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    private static ConnectorDevelopmentType development() {
        var application = new ConnDevApplicationInfoType()
                .applicationName("Test App")
                .description("A test application")
                .version("4.2")
                .apiVersion("v3")
                .integrationType(ConnDevIntegrationType.SCIM)
                .detectedSchema(new ConnDevSchemaType());

        var user = new ConnDevObjectClassInfoType().name("User").relevant(true);
        user.nativeSchemaScript(new ConnDevArtifactType()
                .objectClass("User")
                .operation(ConnDevOperationType.SCHEMA)
                .intent(ConnDevScriptIntentType.NATIVE)
                .filename("User.native.schema.groovy"));

        var connector = new ConnDevConnectorType()
                .groupId("com.example")
                .artifactId("test-connector")
                .version("1.0-SNAPSHOT")
                .integrationType(ConnDevIntegrationType.SCIM)
                .testOperation(new ConnDevArtifactType()
                        .operation(ConnDevOperationType.TEST_CONNECTION)
                        .filename("test.op.groovy"));
        connector.objectClass(user);
        connector.auth(new ConnDevAuthInfoType()
                .name("HTTP Basic Authorization")
                .type(ConnDevHttpAuthTypeType.BASIC));
        connector.auth(new ConnDevAuthInfoType()
                .name("HTTP API Key Authorization")
                .type(ConnDevHttpAuthTypeType.API_KEY)
                .quirks("The key is sent in the X-Api-Key header"));

        return new ConnectorDevelopmentType()
                .name("com.example:test-connector:1.0-SNAPSHOT")
                .application(application)
                .connector(connector);
    }

    @Test
    public void writesApplicationVersionsAndConnectorDetails() throws Exception {
        var manifest = ConnectorManifestReader.read(new ConnectorManifestWriter(development()).serialize());

        assertThat(manifest.application().name()).isEqualTo("Test App");
        assertThat(manifest.application().description()).isEqualTo("A test application");
        assertThat(manifest.application().version()).isEqualTo("4.2");
        assertThat(manifest.application().apiVersion()).isEqualTo("v3");

        assertThat(manifest.integrationType()).isEqualTo(ConnDevIntegrationType.SCIM);
        assertThat(manifest.authMethods()).hasSize(2);
        assertThat(manifest.authMethods().get(0).type()).isEqualTo(ConnDevHttpAuthTypeType.BASIC);
        assertThat(manifest.authMethods().get(0).name()).isEqualTo("HTTP Basic Authorization");
        assertThat(manifest.authMethods().get(0).quirks()).isNull();
        assertThat(manifest.authMethods().get(1).type()).isEqualTo(ConnDevHttpAuthTypeType.API_KEY);
        assertThat(manifest.authMethods().get(1).quirks()).isEqualTo("The key is sent in the X-Api-Key header");

        assertThat(manifest.scripts())
                .extracting(ConnectorManifestReader.ManifestScript::path)
                .contains("/User.native.schema.groovy", "/test.op.groovy");
    }

    @Test
    public void omitsEmptyOptionalSections() throws Exception {
        var development = development();
        development.getApplication().version(null).apiVersion(null);
        development.getConnector().integrationType(null);
        development.getConnector().getAuth().clear();

        var yaml = new ConnectorManifestWriter(development).serialize();

        assertThat(yaml).doesNotContain("version:");
        assertThat(yaml).doesNotContain("apiVersion:");
        assertThat(yaml).doesNotContain("integrationType:");
        assertThat(yaml).doesNotContain("authMethods:");
    }
}
