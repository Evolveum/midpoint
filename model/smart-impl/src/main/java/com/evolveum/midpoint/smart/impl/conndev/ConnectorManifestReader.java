/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevScriptIntentType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the low-code connector manifest ({@code connector.manifest.yaml} / {@code connector.manifest.json})
 * of an existing connector bundle and maps its script entries onto conndev artifact slots, so that
 * an imported {@link com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType}
 * can be prefilled with the connector's existing scripts and object classes.
 *
 * <p>Two manifest dialects are supported:
 * <ul>
 *     <li>the canonical conndev dialect (written by {@link ConnectorManifestWriter}), where each
 *         entry carries {@code objectClass}, {@code operation} and {@code intent} explicitly;</li>
 *     <li>the legacy low-code dialect (hand-written connector projects), where entries carry only
 *         a {@code script} path - the artifact slot is then inferred from the file name.</li>
 * </ul>
 */
public class ConnectorManifestReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorManifestReader.class);

    public static final String MANIFEST_YAML = "connector.manifest.yaml";
    public static final String MANIFEST_JSON = "connector.manifest.json";

    private static final YAMLMapper MAPPER = new YAMLMapper();

    public record ManifestApplication(
            String name,
            String description,
            String version,
            String apiVersion) {
    }

    /**
     * A supported (selected) authentication method of the connector. {@code type} is
     * mandatory; {@code name} and {@code quirks} are optional and only carried over when the
     * manifest provides them.
     */
    public record ManifestAuth(
            ConnDevHttpAuthTypeType type,
            String name,
            String quirks) {
    }

    /**
     * A single script entry of the manifest. For relation schema scripts {@code objectClass}
     * carries the relation name (the same convention conndev uses for relation artifacts).
     */
    public record ManifestScript(
            String path,
            String objectClass,
            ConnDevOperationType operation,
            ConnDevScriptIntentType intent,
            boolean disabled) {
    }

    public record Manifest(
            ManifestApplication application,
            ConnDevIntegrationType integrationType,
            List<ManifestAuth> authMethods,
            List<ManifestScript> scripts) {
    }

    public static Manifest read(String content) throws IOException {
        var root = MAPPER.readTree(content);

        var applicationNode = root.path("application");
        ManifestApplication application = null;
        if (applicationNode.isObject() && !applicationNode.isEmpty()) {
            application = new ManifestApplication(
                    textOrNull(applicationNode.path("name")),
                    textOrNull(applicationNode.path("description")),
                    textOrNull(applicationNode.path("version")),
                    textOrNull(applicationNode.path("apiVersion")));
        }

        var scripts = new ArrayList<ManifestScript>();
        var connectorNode = root.path("connector");
        var integrationType = fromIntegrationType(textOrNull(connectorNode.path("integrationType")));
        var authMethods = new ArrayList<ManifestAuth>();
        var authMethodsNode = connectorNode.path("authMethods");
        if (authMethodsNode.isArray()) {
            for (var entry : authMethodsNode) {
                var auth = parseAuthMethod(entry);
                if (auth != null) {
                    authMethods.add(auth);
                }
            }
        }
        collectScripts(connectorNode.path("schema"), scripts);
        collectScripts(connectorNode.path("authorization"), scripts);
        collectScripts(connectorNode.path("operation"), scripts);

        return new Manifest(application, integrationType, List.copyOf(authMethods), List.copyOf(scripts));
    }

    private static ManifestAuth parseAuthMethod(JsonNode entry) {
        var type = fromAuthType(textOrNull(entry.path("type")));
        if (type == null) {
            return null;
        }
        return new ManifestAuth(type, textOrNull(entry.path("name")), textOrNull(entry.path("quirks")));
    }

    private static void collectScripts(JsonNode section, List<ManifestScript> scripts) {
        if (!section.isArray()) {
            return;
        }
        for (var entry : section) {
            var path = textOrNull(entry.path("script"));
            if (path == null) {
                continue;
            }
            var script = classify(entry, path);
            if (script == null) {
                LOGGER.warn("Couldn't map manifest script '{}' to a known connector artifact slot; it will be ignored", path);
                continue;
            }
            scripts.add(script);
        }
    }

    private static ManifestScript classify(JsonNode entry, String path) {
        var objectClass = textOrNull(entry.path("objectClass"));
        var operation = fromOperation(textOrNull(entry.path("operation")));
        var intent = fromIntent(textOrNull(entry.path("intent")));
        var disabled = entry.path("disabled").asBoolean(false);

        if (objectClass != null && (operation != null || intent != null)) {
            // canonical conndev dialect
            return new ManifestScript(path, objectClass, operation, intent, disabled);
        }

        // legacy dialect - infer the slot from the file name
        var classified = classifyByFilename(path);
        if (classified == null) {
            return null;
        }
        return new ManifestScript(path, classified.objectClass(), classified.operation(), classified.intent(), disabled);
    }

    /**
     * Maps a legacy (classification-less) script file name to an artifact slot:
     * {@code User.native.schema.groovy} → native schema of {@code User},
     * {@code User.search.groovy} / {@code User.search.all.groovy} → search-all of {@code User},
     * {@code User.search.id.groovy} → search-by-id, {@code User.search.filter.groovy} → search-filter,
     * {@code User.create.op.groovy} / {@code update} / {@code delete} → the respective operations,
     * {@code authentication.op.groovy} / {@code authorization.op.groovy} → the authentication script,
     * {@code test.op.groovy} → the test-connection operation, and {@code <relation>.schema.groovy}
     * (e.g. {@code associations.schema.groovy}) → the relation schema script. The object class
     * must be a single (dot-less) name, as produced by the conndev code generation. Returns
     * {@code null} for unrecognized names.
     */
    static ArtifactSlot classifyByFilename(String path) {
        var name = normalizeFilename(path);
        if (name == null) {
            return null;
        }
        var base = name.toLowerCase();
        if (base.equals("authentication") || base.equals("authorization")
                || base.equals("authentication.op") || base.equals("authorization.op")) {
            return new ArtifactSlot(null, null, ConnDevScriptIntentType.AUTH);
        }
        if (base.equals("test") || base.equals("test.op") || base.equals("test.connection")) {
            return new ArtifactSlot(null, ConnDevOperationType.TEST_CONNECTION, null);
        }

        var tokens = name.split("\\.");
        if (tokens.length < 2) {
            return null;
        }
        // The object class keeps its original case (e.g. "User"); only the slot suffix is
        // matched case-insensitively.
        var objectClass = tokens[0];
        var rest = String.join(".", Arrays.copyOfRange(tokens, 1, tokens.length)).toLowerCase();
        switch (rest) {
            case "native.schema":
                return new ArtifactSlot(objectClass, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.NATIVE);
            case "search":
            case "search.all":
                return new ArtifactSlot(objectClass, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ALL);
            case "search.id":
            case "search.by.id":
            case "searchById":
                return new ArtifactSlot(objectClass, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ID);
            case "search.filter":
                return new ArtifactSlot(objectClass, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.FILTER);
            case "create":
            case "create.op":
                return new ArtifactSlot(objectClass, ConnDevOperationType.CREATE, null);
            case "update":
            case "update.op":
                return new ArtifactSlot(objectClass, ConnDevOperationType.UPDATE, null);
            case "delete":
            case "delete.op":
                return new ArtifactSlot(objectClass, ConnDevOperationType.DELETE, null);
            default:
                // relation schema scripts: <relationName>.schema(.groovy|yaml)
                if (rest.equals("schema")) {
                    return new ArtifactSlot(objectClass, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.RELATION);
                }
                return null;
        }
    }

    /** The artifact slot an imported script is mapped to (object class or relation name, operation, intent). */
    public record ArtifactSlot(String objectClass, ConnDevOperationType operation, ConnDevScriptIntentType intent) {
    }

    /**
     * The script file name without directory prefix and without a recognized script extension
     * ({@code .groovy}/{@code .yaml}/{@code .yml}); the original case is preserved.
     */
    private static String normalizeFilename(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        var name = path.trim();
        var slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        var dot = name.lastIndexOf('.');
        if (dot > 0) {
            var extension = name.substring(dot + 1).toLowerCase();
            if (extension.equals("groovy") || extension.equals("yaml") || extension.equals("yml")) {
                name = name.substring(0, dot);
            }
        }
        return name;
    }

    /**
     * The scalar value of the node as text, or {@code null} when absent/blank. Accepts both
     * textual and numeric scalars - hand-written manifests frequently leave values such as
     * {@code version: 4.2} unquoted, and YAML then parses them as numbers.
     */
    private static String textOrNull(JsonNode node) {
        if (node == null || !node.isValueNode() || node.isNull()) {
            return null;
        }
        var text = node.asText();
        return text != null && !text.isBlank() ? text : null;
    }

    private static ConnDevOperationType fromOperation(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ConnDevOperationType.fromValue(value);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown operation value '{}' in connector manifest; treating the entry as legacy", value);
            return null;
        }
    }

    private static ConnDevScriptIntentType fromIntent(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ConnDevScriptIntentType.fromValue(value);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown intent value '{}' in connector manifest; treating the entry as legacy", value);
            return null;
        }
    }

    private static ConnDevIntegrationType fromIntegrationType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ConnDevIntegrationType.fromValue(value);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown integrationType value '{}' in connector manifest; it will be ignored", value);
            return null;
        }
    }

    private static ConnDevHttpAuthTypeType fromAuthType(String value) {
        if (value == null) {
            LOGGER.warn("Auth method without a type in connector manifest; the entry will be ignored");
            return null;
        }
        try {
            return ConnDevHttpAuthTypeType.fromValue(value);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown auth method type '{}' in connector manifest; the entry will be ignored", value);
            return null;
        }
    }
}
