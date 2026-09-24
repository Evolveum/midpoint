/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAttributeInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevBasicObjectClassInfoType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevSchemaObjectClassParser} - the lenient parsing of the
 * development-mode schema model exported through the shared {@code conndev_ObjectClass}
 * dev object class.
 */
public class DevSchemaObjectClassParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode content;

    @BeforeClass
    public void setUp() throws IOException {
        content = MAPPER.readTree("""
                {
                  "name": ["User"],
                  "description": "A user",
                  "embedded": false,
                  "abstract": false,
                  "superclass": "Base",
                  "attributes": [
                    {
                      "name": "id",
                      "type": "string",
                      "primaryKey": true,
                      "autoIncrement": true,
                      "description": "The identifier"
                    },
                    {
                      "name": "userName",
                      "type": "string",
                      "required": true,
                      "multivalue": true
                    },
                    {
                      "name": "email",
                      "jsonType": "email",
                      "multiValued": true,
                      "notNull": true,
                      "readable": false
                    },
                    { "name": null },
                    { "type": "string" }
                  ]
                }
                """);
    }

    @Test
    public void basicObjectClassInfo() {
        var objectClass = DevSchemaObjectClassParser.toBasicObjectClassInfo("User", content);

        assertThat(objectClass).isNotNull();
        assertThat(objectClass.getName()).isEqualTo("User");
        assertThat(objectClass.getDescription()).isEqualTo("A user");
        assertThat(objectClass.getEmbedded()).isFalse();
        assertThat(objectClass.isAbstract()).isFalse();
        assertThat(objectClass.getSuperclass()).isEqualTo("Base");
        assertThat(objectClass.getRelevant()).isTrue();
    }

    @Test
    public void missingFieldsDefaulted() {
        var empty = MAPPER.createObjectNode();
        var objectClass = DevSchemaObjectClassParser.toBasicObjectClassInfo("Minimal", empty);

        assertThat(objectClass).isNotNull();
        assertThat(objectClass.getName()).isEqualTo("Minimal");
        assertThat(objectClass.getDescription()).isNull();
        assertThat(objectClass.getEmbedded()).isFalse();
        assertThat(objectClass.isAbstract()).isFalse();
        assertThat(objectClass.getSuperclass()).isNull();
    }

    @Test
    public void blankNameYieldsNull() {
        assertThat(DevSchemaObjectClassParser.toBasicObjectClassInfo(null, content)).isNull();
        assertThat(DevSchemaObjectClassParser.toBasicObjectClassInfo("  ", content)).isNull();
    }

    @Test
    public void attributes() {
        var attributes = DevSchemaObjectClassParser.toAttributes(content);

        // name-less / missing-name entries are skipped
        assertThat(attributes).hasSize(3);

        var id = attributes.get(0);
        assertThat(id.getName()).isEqualTo("id");
        assertThat(id.getType()).isEqualTo("string");
        assertThat(id.getMandatory()).isTrue(); // primary key
        assertThat(id.getUpdatable()).isFalse(); // auto-increment
        assertThat(id.getCreatable()).isFalse(); // auto-increment
        assertThat(id.getDescription()).isEqualTo("The identifier");

        var userName = attributes.get(1);
        assertThat(userName.getName()).isEqualTo("userName");
        assertThat(userName.getMandatory()).isTrue(); // required
        assertThat(userName.getUpdatable()).isTrue();
        assertThat(userName.getCreatable()).isTrue();
        assertThat(userName.getMultivalue()).isTrue();

        var email = attributes.get(2);
        assertThat(email.getName()).isEqualTo("email");
        assertThat(email.getType()).isEqualTo("email"); // jsonType fallback
        assertThat(email.getMultivalue()).isTrue();
        assertThat(email.getReadable()).isFalse();
        assertThat(email.getMandatory()).isTrue(); // not null
    }

    @Test
    public void noAttributesYieldsEmptyList() {
        var empty = MAPPER.createObjectNode();
        assertThat(DevSchemaObjectClassParser.toAttributes(empty)).isEmpty();
    }

    @Test
    public void polyStringArrays() throws IOException {
        var node = MAPPER.readTree("""
                {
                  "description": ["orig", "translated"],
                  "attributes": [
                    { "name": ["name"], "description": ["d"] }
                  ]
                }
                """);

        var objectClass = DevSchemaObjectClassParser.toBasicObjectClassInfo("User", node);
        assertThat(objectClass.getDescription()).isEqualTo("orig");

        var attributes = DevSchemaObjectClassParser.toAttributes(node);
        assertThat(attributes).hasSize(1);
        assertThat(attributes.get(0).getName()).isEqualTo("name");
        assertThat(attributes.get(0).getDescription()).isEqualTo("d");
    }

    @Test
    public void returnedByDefaultAndTypeFallback() throws IOException {
        var node = MAPPER.readTree("""
                {
                  "attributes": [
                    { "name": "a", "type": "t1" },
                    { "name": "b" }
                  ]
                }
                """);
        var attributes = DevSchemaObjectClassParser.toAttributes(node);
        assertThat(attributes.get(0).getType()).isEqualTo("t1");
        assertThat(attributes.get(0).getReturnedByDefault()).isTrue();
        assertThat(attributes.get(1).getType()).isNull();
    }
}
