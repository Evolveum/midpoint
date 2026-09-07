/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ConnectorDevelopmentArtifacts {

    public enum KnownArtifactType {
        CONFIGURATION_VISIBILITY(false, null, null,
                "configuration-override.properties", null, null),
        AUTHENTICATION_CUSTOMIZATION(false, null, ConnDevScriptIntentType.AUTH,
                "authentication.op.groovy", ConnDevConnectorType.F_AUTHENTICATION_SCRIPT, null),
        SEARCH_ALL_DEFINITION(true, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ALL,
                "search.all.op.groovy", ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION, "SearchAll"),
        SEARCH_BY_ID_DEFINITION(true, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ID,
                "search.id.op.groovy", ConnDevObjectClassInfoType.F_SEARCH_ID_OPERATION, "SearchId"),
        SEARCH_FILTER_DEFINITION(true, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.FILTER,
                "search.filter.op.groovy", ConnDevObjectClassInfoType.F_SEARCH_FILTER_OPERATION, "SearchFilter"),
        NATIVE_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.NATIVE,
                "native.schema.groovy", ConnDevObjectClassInfoType.F_NATIVE_SCHEMA_SCRIPT, "NativeSchema"),
        CONNID_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.CONNID,
                "connid.schema.groovy", ConnDevObjectClassInfoType.F_CONNID_SCHEMA_SCRIPT, "Connid"),
        TEST_CONNECTION_DEFINITION(true, ConnDevOperationType.TEST_CONNECTION, null,
                "test.op.groovy", ConnDevConnectorType.F_TEST_OPERATION, null),
        RELATIONSHIP_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.RELATION,
                "schema.groovy", ConnDevRelationInfoType.F_SCHEMA_SCRIPT, null),
        CREATE(true, ConnDevOperationType.CREATE, null,
                "create.op.groovy", ConnDevObjectClassInfoType.F_CREATE_SCRIPT, "Create"),
        UPDATE(true, ConnDevOperationType.UPDATE, null,
                "update.op.groovy", ConnDevObjectClassInfoType.F_UPDATE_SCRIPT, "Update"),
        DELETE(true, ConnDevOperationType.DELETE, null,
                "delete.op.groovy", ConnDevObjectClassInfoType.F_DELETE_SCRIPT, "Delete")
        ;

        public final ConnDevOperationType operation;
        public final ConnDevScriptIntentType scriptIntent;
        public final boolean objectClassSpecific;
        public final String filenameSuffix;
        public final ItemName itemName;
        public final String fixOperationKeySuffix;

        KnownArtifactType(boolean objectClassSpecific, ConnDevOperationType operation, ConnDevScriptIntentType scriptIntent,
                String filenameSuffix, ItemName itemName, String fixOperationKeySuffix) {
            this.operation = operation;
            this.scriptIntent = scriptIntent;
            this.objectClassSpecific = objectClassSpecific;
            this.filenameSuffix = filenameSuffix;
            this.itemName = itemName;
            this.fixOperationKeySuffix = fixOperationKeySuffix;
        }

        public ConnDevArtifactType create(String objectClass) {
            var filename = objectClassSpecific ? objectClass + "." + filenameSuffix : filenameSuffix;
            return new ConnDevArtifactType()
                    .objectClass(objectClass)
                    .filename(filename)
                    .intent(scriptIntent)
                    .operation(operation);
        }

        public ConnDevArtifactType create() {
            if (objectClassSpecific) {
                throw new IllegalStateException("Cannot create an object class for an object class specifc");
            }
            return create(null);
        }
    }

    public static ConnDevArtifactType authenticationScript() {
        return KnownArtifactType.AUTHENTICATION_CUSTOMIZATION.create(null);
    }

    public static ConnDevArtifactType testConnectionScript() {
        return KnownArtifactType.TEST_CONNECTION_DEFINITION.create(null);
    }

    public static KnownArtifactType classify(ConnDevArtifactType artifactSpec) {
        var classification = Arrays.stream(KnownArtifactType.values()).filter(
                at -> Objects.equals(at.scriptIntent, artifactSpec.getIntent())
                        && Objects.equals(at.operation, artifactSpec.getOperation()))
                .findFirst();

        return classification.orElse(null);
    }

    /**
     * Reverses {@link KnownArtifactType#fixOperationKeySuffix}: strips the object class name off
     * the front of a {@code /fix} response's {@code operationKey} and matches what's left against
     * every type's suffix, case-insensitively.
     */
    public static KnownArtifactType classifyByFixOperationKey(String operationKey, String objectClass) {
        if (operationKey == null || objectClass == null
                || operationKey.length() <= objectClass.length()
                || !operationKey.regionMatches(true, 0, objectClass, 0, objectClass.length())) {
            return null;
        }
        var suffix = operationKey.substring(objectClass.length());
        return Arrays.stream(KnownArtifactType.values())
                .filter(t -> t.fixOperationKeySuffix != null && t.fixOperationKeySuffix.equalsIgnoreCase(suffix))
                .findFirst()
                .orElse(null);
    }

    /**
     * Every script artifact declared anywhere on the connector - object class schema/search/
     * create/update/delete scripts, relation schema scripts, the authentication script, and the
     * test-connection operation - in a fixed, deterministic order, skipping unset slots. The
     * single source both {@code ConnectorManifestWriter} and any filename-based artifact lookup
     * (e.g. resolving a validation error's source file back to its artifact) should use, instead
     * of separately re-enumerating the same fields.
     */
    public static List<ConnDevArtifactType> allArtifacts(ConnDevConnectorType connector) {
        var artifacts = new ArrayList<ConnDevArtifactType>();
        addIfPresent(artifacts, connector.getAuthenticationScript());
        addIfPresent(artifacts, connector.getTestOperation());
        for (var objClass : connector.getObjectClass()) {
            addIfPresent(artifacts, objClass.getNativeSchemaScript());
            addIfPresent(artifacts, objClass.getConnidSchemaScript());
            addIfPresent(artifacts, objClass.getSearchAllOperation());
            addIfPresent(artifacts, objClass.getSearchIdOperation());
            addIfPresent(artifacts, objClass.getSearchFilterOperation());
            addIfPresent(artifacts, objClass.getCreateScript());
            addIfPresent(artifacts, objClass.getUpdateScript());
            addIfPresent(artifacts, objClass.getDeleteScript());
        }
        for (var relation : connector.getRelation()) {
            addIfPresent(artifacts, relation.getSchemaScript());
        }
        return artifacts;
    }

    private static void addIfPresent(List<ConnDevArtifactType> artifacts, ConnDevArtifactType artifact) {
        if (artifact != null) {
            artifacts.add(artifact);
        }
    }
}
