/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.model.intest.smart.TestSmartIntegrationService.OC_ACCOUNT_QNAME;

/**
 * Service client to be used when there is no real service available.
 */
public class MockServiceClientImpl implements ServiceClient {

    @Override
    public ObjectTypesSuggestionType suggestObjectTypes(
            ResourceObjectClassDefinition objectClassDef,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            ResourceSchema resourceSchema,
            Task task,
            OperationResult result) {
        return new ObjectTypesSuggestionType()
                .objectType(new ObjectTypeSuggestionType()
                        .identification(new ResourceObjectTypeIdentificationType()
                                .kind(ShadowKindType.ACCOUNT)
                                .intent(SchemaConstants.INTENT_DEFAULT))
                        .delineation(new ResourceObjectTypeDelineationType()
                                .objectClass(OC_ACCOUNT_QNAME)));
    }

    @Override
    public QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            Task task,
            OperationResult result) {
        return switch (typeIdentification.getKind()) {
            case ACCOUNT -> UserType.COMPLEX_TYPE;
            case ENTITLEMENT -> RoleType.COMPLEX_TYPE;
            case GENERIC -> OrgType.COMPLEX_TYPE;
            default -> throw new AssertionError(typeIdentification);
        };
    }

    @Override
    public void close() {
    }
}
