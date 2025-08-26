/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevScriptIntentType;

public class ConnectorArtifact {



    enum KnownArtifactType {
        CONFIGURATION_VISIBILITY(false, null, null, "configuration-override.properties"),
        AUTHENTICATION_CUSTOMIZATION(false, null, ConnDevScriptIntentType.AUTH, "authentication.groovy"),
        SEARCH_ALL_DEFINITION(true, ConnDevOperationType.SEARCH, ConnDevScriptIntentType.ALL, "search.all.op.groovy"),
        GET_BY_UID_DEFINITION(true, ConnDevOperationType.GET, null, "get.op.groovy" ),
        NATIVE_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.NATIVE, ".native.schema.groovy"),
        CONNID_SCHEMA_DEFINITION(true, ConnDevOperationType.SCHEMA, ConnDevScriptIntentType.NATIVE, ".connid.schema.groovy"),
        TEST_CONNECTION_DEFINITION(true, ConnDevOperationType.TEST_CONNECTION, null, "testConnection.groovy"),
        ;

        KnownArtifactType(boolean objectClassSpecific, ConnDevOperationType operation, ConnDevScriptIntentType scriptIntent, String filenameSuffix) {
            this.operation = operation;
            this.scriptIntent = scriptIntent;
            this.objectClassSpecific = objectClassSpecific;
            this.filenameSuffix = filenameSuffix;
        }

        public final ConnDevOperationType operation;
        public final ConnDevScriptIntentType scriptIntent;
        public final boolean objectClassSpecific;
        public final String filenameSuffix;
    }




    ConnDevArtifactType artifact;




}
