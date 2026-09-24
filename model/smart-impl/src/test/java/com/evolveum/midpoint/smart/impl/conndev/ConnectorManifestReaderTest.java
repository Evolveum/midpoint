/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevScriptIntentType;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ConnectorManifestReader} - both the canonical conndev manifest dialect
 * and the legacy low-code dialect (script path only, slot inferred from the file name).
 */
public class ConnectorManifestReaderTest {

    private static final String CANONICAL_YAML = """
            application:
              name: Test App
              description: A test application
            connector:
              schema:
                - script: /User.native.schema.groovy
                  objectClass: User
                  operation: schema
                  intent: native
                - script: /associations.schema.groovy
                  objectClass: associations
                  operation: schema
                  intent: relation
              authorization:
                - script: /authentication.op.groovy
                  intent: auth
                - script: /User.search.groovy
                  objectClass: User
                  operation: search
                  intent: all
              operation:
                - script: /test.op.groovy
                  operation: testConnection
                - script: /User.create.op.groovy
                  objectClass: User
                  operation: create
                - script: /User.update.op.groovy
                  objectClass: User
                  operation: update
                - script: /User.delete.op.groovy
                  objectClass: User
                  operation: delete
                  disabled: true
                - script: /User.search.id.groovy
                  objectClass: User
                  operation: search
                  intent: id
                - script: /User.search.filter.groovy
                  objectClass: User
                  operation: search
                  intent: filter
            """;

    @Test
    public void canonicalDialect() throws Exception {
        var manifest = ConnectorManifestReader.read(CANONICAL_YAML);

        assertThat(manifest.application().name()).isEqualTo("Test App");
        assertThat(manifest.application().description()).isEqualTo("A test application");

        assertThat(manifest.scripts()).hasSize(10);

        var userSchema = manifest.scripts().stream()
                .filter(s -> s.operation() == ConnDevOperationType.SCHEMA && s.intent() == ConnDevScriptIntentType.NATIVE)
                .findFirst().orElse(null);
        assertThat(userSchema).isNotNull();
        assertThat(userSchema.objectClass()).isEqualTo("User");
        assertThat(userSchema.path()).isEqualTo("/User.native.schema.groovy");
        assertThat(userSchema.disabled()).isFalse();

        var relation = manifest.scripts().stream()
                .filter(s -> s.intent() == ConnDevScriptIntentType.RELATION)
                .findFirst().orElse(null);
        assertThat(relation).isNotNull();
        assertThat(relation.objectClass()).isEqualTo("associations");

        var auth = manifest.scripts().stream()
                .filter(s -> s.intent() == ConnDevScriptIntentType.AUTH)
                .findFirst().orElse(null);
        assertThat(auth).isNotNull();
        assertThat(auth.operation()).isNull();

        var test = manifest.scripts().stream()
                .filter(s -> s.operation() == ConnDevOperationType.TEST_CONNECTION)
                .findFirst().orElse(null);
        assertThat(test).isNotNull();
        assertThat(test.objectClass()).isNull();

        var disabled = manifest.scripts().stream()
                .filter(s -> s.operation() == ConnDevOperationType.DELETE)
                .findFirst().orElse(null);
        assertThat(disabled).isNotNull();
        assertThat(disabled.disabled()).isTrue();

        assertThat(manifest.scripts().stream()
                .filter(s -> s.operation() == ConnDevOperationType.SEARCH)
                .map(s -> s.intent())
                .sorted())
                .containsExactly(
                        ConnDevScriptIntentType.ALL,
                        ConnDevScriptIntentType.ID,
                        ConnDevScriptIntentType.FILTER);
    }

    @Test
    public void legacyDialect() throws Exception {
        var manifest = ConnectorManifestReader.read("""
                connector:
                  schema:
                    - script: User.native.schema.groovy
                    - script: associations.schema.groovy
                  authorization:
                    - script: authentication.op.groovy
                  operation:
                    - script: test.op.groovy
                    - script: User.search.groovy
                    - script: User.search.id.groovy
                    - script: User.search.filter.groovy
                    - script: User.create.op.groovy
                    - script: User.update.op.groovy
                    - script: User.delete.op.groovy
                """);

        assertThat(manifest.application()).isNull();

        var userSchema = script(manifest, "User", ConnDevOperationType.SCHEMA);
        assertThat(userSchema.intent()).isEqualTo(ConnDevScriptIntentType.NATIVE);

        var relation = script(manifest, "associations", ConnDevOperationType.SCHEMA);
        assertThat(relation.intent()).isEqualTo(ConnDevScriptIntentType.RELATION);

        var auth = manifest.scripts().stream()
                .filter(s -> s.intent() == ConnDevScriptIntentType.AUTH)
                .findFirst().orElse(null);
        assertThat(auth).isNotNull();
        assertThat(auth.objectClass()).isNull();
        assertThat(auth.operation()).isNull();

        var test = script(manifest, null, ConnDevOperationType.TEST_CONNECTION);
        assertThat(test).isNotNull();

        var searchAll = script(manifest, "User", ConnDevOperationType.SEARCH);
        assertThat(searchAll.intent()).isEqualTo(ConnDevScriptIntentType.ALL);

        assertThat(script(manifest, "User", ConnDevOperationType.CREATE)).isNotNull();
        assertThat(script(manifest, "User", ConnDevOperationType.UPDATE)).isNotNull();
        assertThat(script(manifest, "User", ConnDevOperationType.DELETE)).isNotNull();
    }

    @Test
    public void jsonFormat() throws Exception {
        var manifest = ConnectorManifestReader.read("""
                {
                  "application": { "name": "Json App" },
                  "connector": {
                    "operation": [
                      { "script": "test.op.groovy", "operation": "testConnection" },
                      { "script": "User.create.op.groovy", "objectClass": "User", "operation": "create" }
                    ]
                  }
                }
                """);

        assertThat(manifest.application().name()).isEqualTo("Json App");
        assertThat(manifest.scripts()).hasSize(2);
        assertThat(script(manifest, "User", ConnDevOperationType.CREATE).path()).isEqualTo("User.create.op.groovy");
    }

    @Test
    public void unknownScriptIgnored() throws Exception {
        var manifest = ConnectorManifestReader.read("""
                connector:
                  operation:
                    - script: something.unknown.file.xyz
                """);
        assertThat(manifest.scripts()).isEmpty();
    }

    @Test
    public void invalidContent() {
        assertThatThrownBy(() -> ConnectorManifestReader.read(":\n  - not: [valid"))
                .isInstanceOf(Exception.class);
    }

    private static ConnectorManifestReader.ManifestScript script(
            ConnectorManifestReader.Manifest manifest, String objectClass, ConnDevOperationType operation) {
        return manifest.scripts().stream()
                .filter(s -> objectClass == null ? s.objectClass() == null : objectClass.equals(s.objectClass()))
                .filter(s -> s.operation() == operation)
                .findFirst().orElse(null);
    }
}
