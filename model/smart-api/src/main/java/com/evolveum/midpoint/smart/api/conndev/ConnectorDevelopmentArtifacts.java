/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Arrays;
import java.util.Objects;

public class ConnectorDevelopmentArtifacts {

    public enum KnownArtifactType {
        CONFIGURATION_VISIBILITY(false, null, null,
                "configuration-override.properties", null),
        AUTHENTICATION_CUSTOMIZATION(false, null, ConnDevScriptIntentType.AUTH,
                "authentication.groovy", ConnDevConnectorType.F_AUTHENTICATION_SCRIPT),
        SEARCH_ALL_DEFINITION(true, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ALL,
                "search.all.op.groovy", ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION),
        GET_BY_UID_DEFINITION(true, ConnDevOperationType.GET, null,
                "get.op.groovy", ConnDevObjectClassInfoType.F_GET_OPERATION),
        NATIVE_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.NATIVE,
                "native.schema.groovy", ConnDevObjectClassInfoType.F_NATIVE_SCHEMA_SCRIPT),
        CONNID_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.CONNID,
                "connid.schema.groovy", ConnDevObjectClassInfoType.F_CONNID_SCHEMA_SCRIPT),
        TEST_CONNECTION_DEFINITION(true, ConnDevOperationType.TEST_CONNECTION, null,
                "testConnection.groovy", ConnDevConnectorType.F_TEST_OPERATION),
        ;

        public final ConnDevOperationType operation;
        public final ConnDevScriptIntentType scriptIntent;
        public final boolean objectClassSpecific;
        public final String filenameSuffix;
        public final ItemName itemName;

        KnownArtifactType(boolean objectClassSpecific, ConnDevOperationType operation, ConnDevScriptIntentType scriptIntent, String filenameSuffix, ItemName itemName) {
            this.operation = operation;
            this.scriptIntent = scriptIntent;
            this.objectClassSpecific = objectClassSpecific;
            this.filenameSuffix = filenameSuffix;
            this.itemName = itemName;
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
}
