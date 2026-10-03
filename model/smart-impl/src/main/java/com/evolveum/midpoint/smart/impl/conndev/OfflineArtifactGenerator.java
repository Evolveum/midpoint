/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.smart.api.conndev.ConnDevScriptFormat;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAttributeInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generates the near-bare script skeletons the offline backends return instead of AI-generated
 * scripts. The skeletons are always *parseable* framework scripts (only documented keys, with
 * placeholder endpoints/implementation blocks) so the user can edit them in the wizard; they are
 * intentionally incomplete and fail script validation until edited.
 *
 * <p>The content is kept minimal on purpose - only what the framework needs to load the script
 * plus whatever can be derived locally: for SCIM/SQL the attributes detected through the
 * development-mode ({@code conndev_ObjectClass}) metadata, and the ConnId mappings where the
 * framework default is unambiguous (SCIM {@code id} to {@code UID}, {@code userName} to
 * {@code NAME}; SQL primary key to {@code __UID__}).
 */
public final class OfflineArtifactGenerator {

    private static final String EOL = "\n";
    private static final Pattern SAFE_YAML_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private OfflineArtifactGenerator() {
    }

    /**
     * Generates the skeleton for the given artifact classification (may be {@code null} for an
     * unrecognized artifact - a commented placeholder is produced then).
     */
    public static ConnDevArtifactType generate(OfflineBackend backend,
            ConnDevGenerateArtifactDefinitionType input,
            ConnectorDevelopmentArtifacts.KnownArtifactType classification) {
        var artifactSpec = input.getArtifact();
        var objectClass = artifactSpec.getObjectClass();
        var isSql = backend instanceof OfflineSqlBackend;
        var isScim = backend instanceof OfflineScimBackend;

        var content = (classification == null)
                ? groovyPlaceholder("script")
                : switch (classification) {
                    case AUTHENTICATION_CUSTOMIZATION -> isSql
                            ? groovyPlaceholder("authentication")
                            : authentication(isScim, backend);
                    case TEST_CONNECTION_DEFINITION -> testConnection(isSql);
                    case NATIVE_SCHEMA_DEFINITION -> nativeSchema(backend, objectClass, isSql, isScim);
                    case SEARCH_ALL_DEFINITION -> isSql
                            ? sqlNoOp(objectClass, "search", "Search works out of the box for discovered tables")
                            : restOperation(objectClass, "search",
                                    List.of("- method: GET", "  path: /" + placeholderPath(objectClass),
                                            "  # TODO (offline skeleton): set the list/search endpoint path"));
                    case SEARCH_BY_ID_DEFINITION -> isSql
                            ? sqlNoOp(objectClass, "search", "Search works out of the box for discovered tables")
                            : restOperation(objectClass, "search",
                                    List.of("- method: GET", "  path: /" + placeholderPath(objectClass) + "/{id}",
                                            "  singleResult: true",
                                            "  # TODO (offline skeleton): set the get-by-id endpoint path"));
                    case SEARCH_FILTER_DEFINITION -> isSql
                            ? sqlNoOp(objectClass, "search", "Search works out of the box for discovered tables")
                            : restOperation(objectClass, "search",
                                    List.of("- method: GET", "  path: /" + placeholderPath(objectClass),
                                            "  # TODO (offline skeleton): set the search endpoint path and the supported filters"));
                    case CREATE -> isSql
                            ? sqlOperation(objectClass, "create")
                            : restOperation(objectClass, "create",
                                    List.of("- method: POST", "  path: /" + placeholderPath(objectClass),
                                            "  # TODO (offline skeleton): set the create endpoint path"));
                    case UPDATE -> isSql
                            ? sqlOperation(objectClass, "update")
                            : restOperation(objectClass, "update",
                                    List.of("- method: PATCH", "  path: /" + placeholderPath(objectClass) + "/{id}",
                                            "  # TODO (offline skeleton): set the update endpoint path"));
                    case DELETE -> isSql
                            ? sqlOperation(objectClass, "delete")
                            : restOperation(objectClass, "delete",
                                    List.of("- method: DELETE", "  path: /" + placeholderPath(objectClass) + "/{id}",
                                            "  # TODO (offline skeleton): set the delete endpoint path"));
                    case RELATIONSHIP_SCHEMA_DEFINITION -> groovyRelationship(objectClass);
                    default -> groovyPlaceholder(classification.name());
                };

        var format = (classification == ConnectorDevelopmentArtifacts.KnownArtifactType.TEST_CONNECTION_DEFINITION
                || classification == ConnectorDevelopmentArtifacts.KnownArtifactType.RELATIONSHIP_SCHEMA_DEFINITION
                || classification == null)
                ? ConnDevScriptFormat.GROOVY
                : ConnDevScriptFormat.YAML;
        return artifactSpec
                .filename(format.withExtension(artifactSpec.getFilename()))
                .content(content);
    }

    // -----------------------------------------------------------------
    // SCIM/REST skeletons
    // -----------------------------------------------------------------

    private static String authentication(boolean scim, OfflineBackend backend) {
        var sb = new StringBuilder(yamlHeader("authentication"));
        var namespace = scim ? "scim" : "rest";
        sb.append("authentication:").append(EOL);
        sb.append("  ").append(namespace).append(":").append(EOL);
        List<ConnDevAuthInfoType> auths = backend.developmentObject().getConnector() != null
                ? backend.developmentObject().getConnector().getAuth()
                : List.of();
        var emitted = false;
        for (var auth : auths) {
            var key = yamlAuthKey(auth.getType());
            if (key == null) {
                continue;
            }
            if (!emitted) {
                sb.append("    # TODO (offline skeleton): review the selected authentication methods and").append(EOL);
                sb.append("    # customize their requests where the default behavior is not enough.").append(EOL);
                sb.append("    # customize their requests where the default behavior is not enough.").append(EOL);
            }
            sb.append("    ").append(key).append(":").append(EOL);
            sb.append("      # implementation: |").append(EOL);
            sb.append("      #  // TODO (offline skeleton): customize the request for the \"").append(key)
                    .append("\" method (optional).").append(EOL);
            emitted = true;
        }
        if (!emitted) {
            sb.append("    # TODO (offline skeleton): select the authentication methods and configure them here.").append(EOL);
        }
        return sb.toString();
    }

    private static String nativeSchema(OfflineBackend backend, String objectClass, boolean sql, boolean scim) {
        var sb = new StringBuilder(yamlHeader("native schema"));
        var attributes = objectClassAttributes(backend, objectClass);
        var primaryKeyNames = sql ? primaryKeyNames(backend, objectClass) : List.of();

        sb.append("objectClasses:").append(EOL);
        sb.append("  ").append(yamlKey(objectClass)).append(":").append(EOL);
        if (sql && primaryKeyNames.isEmpty()) {
            sb.append("    # TODO (offline skeleton): map the primary key attribute to __UID__, e.g. add").append(EOL);
            sb.append("    #   connId: { name: __UID__ } to the primary key attribute below.").append(EOL);
        }
        if (attributes.isEmpty()) {
            sb.append("    # TODO (offline skeleton): no attributes were detected - add them here.").append(EOL);
            sb.append("    attributes: {}").append(EOL);
            return sb.toString();
        }
        sb.append("    attributes:").append(EOL);
        for (var attribute : attributes) {
            var name = attribute.getName();
            sb.append("      ").append(yamlKey(name)).append(":").append(EOL);
            var emittedKey = false;
            if (!sql) {
                var type = attribute.getType();
                if (type != null && !type.isBlank()) {
                    sb.append("        jsonType: ").append(yamlString(type)).append(EOL);
                    emittedKey = true;
                }
                var description = attribute.getDescription();
                if (description != null && !description.isBlank()) {
                    sb.append("        description: ").append(yamlString(description)).append(EOL);
                    emittedKey = true;
                }
                appendFlag(sb, "required", attribute.isMandatory(), Boolean.FALSE);
                appendFlag(sb, "multiValued", attribute.isMultivalue(), Boolean.FALSE);
                appendFlag(sb, "creatable", attribute.isCreatable(), Boolean.TRUE);
                appendFlag(sb, "updateable", attribute.isUpdatable(), Boolean.TRUE);
                appendFlag(sb, "readable", attribute.isReadable(), Boolean.TRUE);
                appendFlag(sb, "returnedByDefault", attribute.isReturnedByDefault(), Boolean.TRUE);
                if (scim && "id".equalsIgnoreCase(name)) {
                    sb.append("        connId:").append(EOL);
                    sb.append("          name: UID").append(EOL);
                    emittedKey = true;
                } else if (scim && "userName".equalsIgnoreCase(name)) {
                    sb.append("        connId:").append(EOL);
                    sb.append("          name: NAME").append(EOL);
                    emittedKey = true;
                }
                if (!emittedKey) {
                    sb.append("        # TODO (offline skeleton): review the attribute settings").append(EOL);
                }
            } else {
                if (primaryKeyNames.contains(name.toLowerCase(Locale.ROOT))) {
                    sb.append("        connId:").append(EOL);
                    sb.append("          name: __UID__").append(EOL);
                }
            }
        }
        return sb.toString();
    }

    private static String restOperation(String objectClass, String operationName, List<String> endpointLines) {
        var sb = new StringBuilder(yamlHeader(operationName));
        sb.append("objectClasses:").append(EOL);
        sb.append("  ").append(yamlKey(objectClass)).append(":").append(EOL);
        sb.append("    ").append(operationName).append(":").append(EOL);
        sb.append("      endpoints:").append(EOL);
        for (var line : endpointLines) {
            sb.append("        ").append(line).append(EOL);
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // SQL skeletons
    // -----------------------------------------------------------------

    /** A SQL search skeleton - search works out of the box for discovered tables, nothing to declare. */
    private static String sqlNoOp(String objectClass, String operationName, String note) {
        var sb = new StringBuilder(yamlHeader(operationName));
        sb.append("objectClasses:").append(EOL);
        sb.append("  ").append(yamlKey(objectClass)).append(":").append(EOL);
        sb.append("    # ").append(note).append(" (offline skeleton - no AI service configured).").append(EOL);
        return sb.toString();
    }

    /** A SQL create/update/delete skeleton - the operation is disabled until the user configures it. */
    private static String sqlOperation(String objectClass, String operationName) {
        var sb = new StringBuilder(yamlHeader(operationName));
        sb.append("objectClasses:").append(EOL);
        sb.append("  ").append(yamlKey(objectClass)).append(":").append(EOL);
        sb.append("    ").append(operationName).append(":").append(EOL);
        sb.append("      enabled: false").append(EOL);
        sb.append("      # TODO (offline skeleton): enable and configure the operation when the table supports it.").append(EOL);
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Groovy skeletons (no YAML form in the frameworks)
    // -----------------------------------------------------------------

    private static String testConnection(boolean sql) {
        var sb = new StringBuilder(groovyHeader("test connection"));
        sb.append("test {").append(EOL);
        if (sql) {
            sb.append("    // TODO (offline skeleton): verify the database connection here.").append(EOL);
        } else {
            sb.append("    // TODO (offline skeleton): specify the endpoint used to test the connection, e.g.:").append(EOL);
            sb.append("    // endpoint(\"/health\")").append(EOL);
        }
        sb.append("}").append(EOL);
        return sb.toString();
    }

    private static String groovyRelationship(String objectClass) {
        var sb = new StringBuilder(groovyHeader("relationship"));
        sb.append("relationship(\"").append(objectClass).append("\") {").append(EOL);
        sb.append("    // TODO (offline skeleton): configure the relationship (subject/object attributes) and operations.").append(EOL);
        sb.append("}").append(EOL);
        return sb.toString();
    }

    private static String groovyPlaceholder(String kind) {
        return groovyHeader(kind) + "// TODO (offline skeleton): edit this script (no AI service configured)." + EOL;
    }

    // -----------------------------------------------------------------
    // Data lookup
    // -----------------------------------------------------------------

    /**
     * The attributes for the object class: the stored (development-mode or documentation-derived)
     * attributes first - no testing-resource access needed - falling back to the development-mode
     * metadata (one testing-resource refresh) when nothing is stored yet.
     */
    private static List<ConnDevAttributeInfoType> objectClassAttributes(OfflineBackend backend, String objectClass) {
        var connectorClass = backend.connectorObjectClass(objectClass);
        if (connectorClass != null && connectorClass.getAttribute() != null && !connectorClass.getAttribute().isEmpty()) {
            return connectorClass.getAttribute();
        }
        return backend.discoverObjectClassAttributesFromDevMode(objectClass);
    }

    /**
     * The attribute names that carry the SQL primary key flag in the raw development-mode shadow
     * content (when the flag is exported). Empty when the flag is not available.
     */
    private static List<String> primaryKeyNames(OfflineBackend backend, String objectClass) {
        var ret = new ArrayList<String>();
        for (var shadow : backend.devModeObjectClassShadows()) {
            if (!objectClass.equalsIgnoreCase(shadow.name())) {
                continue;
            }
            var attributes = shadow.content().path("attributes");
            if (attributes.isArray()) {
                for (var attribute : attributes) {
                    var name = attribute.path("name").asText(null);
                    if (name != null
                            && (attribute.path("primaryKey").asBoolean(false) || attribute.path("autoIncrement").asBoolean(false))) {
                        ret.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
            break;
        }
        return ret;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static String placeholderPath(String objectClass) {
        return objectClass != null ? objectClass.toLowerCase(Locale.ROOT) : "resource";
    }

    private static String yamlHeader(String kind) {
        return "# Skeleton " + kind + " template  (no AI service configured)." + EOL
                + "# Documentation is available  in the sidepanel and on https://docs.evolveum.com " + EOL
                + "# Review and edit this script before saving." + EOL;
    }

    private static String groovyHeader(String kind) {
        return "// Skeleton " + kind + " generated by the midPoint connector development (no AI service configured)." + EOL
                + "// Documentation is available  in the sidepanel and on https://docs.evolveum.com" + EOL
                + "// Review and edit this script before saving." + EOL;
    }

    private static void appendFlag(StringBuilder sb, String key, Boolean value, Boolean frameworkDefault) {
        if (value != null && !value.equals(frameworkDefault)) {
            sb.append("        ").append(key).append(": ").append(value).append(EOL);
        }
    }

    /** YAML mapping key: quoted only when it is not a plain identifier. */
    private static String yamlKey(String key) {
        if (key == null) {
            return "";
        }
        return SAFE_YAML_KEY.matcher(key).matches() ? key : yamlString(key);
    }

    /** YAML double-quoted scalar with escaping. */
    private static String yamlString(String value) {
        var sb = new StringBuilder("\"");
        for (var c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    private static String yamlAuthKey(ConnDevHttpAuthTypeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case BASIC -> "basic";
            case BEARER -> "bearer";
            case API_KEY -> "apiKey";
            case JWT_BEARER -> "jwtBearer";
            case OAUTH2_CLIENT_CREDENTIALS -> "oauth2ClientCredentials";
            case OAUTH2_PASSWORD -> "oauth2Password";
            case OAUTH2_JWT -> "oauth2JwtBearer";
            case OAUTH2_SAML -> "oauth2Saml";
            case AWS_SIGNATURE -> "awsSignature";
            default -> null; // digest/hawk/ntlm/other have no YAML auth block
        };
    }
}
