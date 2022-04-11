/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * EXPERIMENTAL
 *
 * TODO make interface generic and integrate it into model API
 */
@FunctionalInterface
public interface ResourceValidator {

    String CAT_BASIC = "basic";
    String CAT_CONFIGURATION = "configuration";
    String CAT_SCHEMA = "schema";
    String CAT_SCHEMA_HANDLING = "schemaHandling";
    String CAT_SYNCHRONIZATION = "synchronization";
    String CAT_CAPABILITIES = "capabilities";

    String C_NO_SCHEMA = "noSchema";
    String C_MISSING_OBJECT_CLASS = "missingObjectClass";
    String C_UNKNOWN_OBJECT_CLASS = "unknownObjectClass";
    String C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS = "multipleSchemaHandlingDefinitions";
    String C_MULTIPLE_SCHEMA_HANDLING_DEFAULT_DEFINITIONS = "multipleSchemaHandlingDefaultDefinitions";
    String C_NO_DEFAULT_ACCOUNT_SCHEMA_HANDLING_DEFAULT_DEFINITION = "noDefaultAccountSchemaHandlingDefinition";
    String C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS = "multipleSynchronizationDefinitions";
    String C_NO_SYNCHRONIZATION_DEFINITION = "noSynchronizationDefinition";
    String C_NO_SCHEMA_HANDLING_DEFINITION = "noSchemaHandlingDefinition";
    String C_NO_ATTRIBUTE_REF = "noAttributeName";
    String C_UNKNOWN_ATTRIBUTE_NAME = "unknownAttributeName";
    String C_COLLIDING_ASSOCIATION_NAME = "collidingAssociationName";
    String C_NO_ASSOCIATION_NAME = "noAssociationName";
    String C_WRONG_ITEM_NAME = "wrongItemName";
    String C_NO_ITEM_NAMESPACE = "noItemNamespace";
    String C_MISSING_ASSOCIATION_TARGET_KIND = "missingAssociationTargetKind";
    String C_MISSING_ASSOCIATION_TARGET_INTENT = "missingAssociationTargetIntent";
    String C_MISSING_ASSOCIATION_DIRECTION = "missingAssociationDirection";
    String C_MISSING_ASSOCIATION_ASSOCIATION_ATTRIBUTE = "missingAssociationAssociationAttribute";
    String C_MISSING_ASSOCIATION_VALUE_ATTRIBUTE = "missingAssociationValueAttribute";
    String C_WRONG_MATCHING_RULE = "wrongMatchingRule";
    String C_MULTIPLE_ITEMS = "multipleItems";
    String C_DEPENDENT_OBJECT_TYPE_DOES_NOT_EXIST = "dependentObjectTypeDoesNotExist";
    String C_TARGET_OBJECT_TYPE_DOES_NOT_EXIST = "targetObjectTypeDoesNotExist";
    String C_INVALID_MAPPING_SOURCE = "invalidMappingSource";
    String C_SUSPICIOUS_MAPPING_SOURCE = "suspiciousMappingSource";
    String C_MISSING_MAPPING_SOURCE = "missingMappingSource";
    String C_INVALID_MAPPING_TARGET = "invalidMappingTarget";
    String C_SUSPICIOUS_MAPPING_TARGET = "suspiciousMappingTarget";
    String C_MISSING_MAPPING_TARGET = "missingMappingTarget";
    String C_SUPERFLUOUS_MAPPING_TARGET = "superfluousMappingTarget";
    String C_UNKNOWN_OBJECT_CLASS_IN_SYNCHRONIZATION = "unknownObjectClassInSynchronization";
    String C_NO_REACTION = "noReaction";
    String C_DUPLICATE_REACTIONS = "duplicateReactions";
    String C_NO_SITUATION = "noSituation";
    String C_NO_CORRELATION_RULE = "noCorrelationRule";

    @NotNull
    ValidationResult validate(@NotNull PrismObject<ResourceType> resourceObject, @NotNull Scope scope,
            @Nullable Locale locale, @NotNull Task task, @NotNull OperationResult result);
}
