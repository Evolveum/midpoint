/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.argNonNull;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Util methods related to {@link SmartIntegrationArtifactType} objects.
 *
 * Namely, they deal with creating such objects (with provided content), and extracting content from them.
 *
 * In particular, it handles the archetypes for these objects.
 */
public class SmartIntegrationArtifactUtil {

    public static final ItemPath PATH_SCOPE_RESOURCE_REF =
            ItemPath.create(
                    SmartIntegrationArtifactType.F_SCOPE,
                    SmartIntegrationArtifactScopeType.F_RESOURCE_REF);
    public static final ItemPath PATH_SCOPE_OBJECT_CLASS =
            ItemPath.create(
                    SmartIntegrationArtifactType.F_SCOPE,
                    SmartIntegrationArtifactScopeType.F_OBJECT_CLASS);
    public static final ItemPath PATH_SCOPE_FOCUS_TYPE =
            ItemPath.create(
                    SmartIntegrationArtifactType.F_SCOPE,
                    SmartIntegrationArtifactScopeType.F_FOCUS_TYPE);
    public static final ItemPath PATH_SCOPE_KIND =
            ItemPath.create(
                    SmartIntegrationArtifactType.F_SCOPE,
                    SmartIntegrationArtifactScopeType.F_OBJECT_TYPE,
                    ResourceObjectTypeIdentificationType.F_KIND);
    public static final ItemPath PATH_SCOPE_INTENT =
            ItemPath.create(
                    SmartIntegrationArtifactType.F_SCOPE,
                    SmartIntegrationArtifactScopeType.F_OBJECT_TYPE,
                    ResourceObjectTypeIdentificationType.F_INTENT);

    public static ObjectSetStatisticsType getStatisticsRequired(SmartIntegrationArtifactType holder) {
        return getStatisticsRequired(holder.asPrismObject());
    }

    public static ObjectSetStatisticsType getStatisticsRequired(PrismObject<SmartIntegrationArtifactType> holder) {
        return argNonNull(holder.asObjectable().getStatistics(), "No statistics in %s", holder);
    }

    public static SmartIntegrationArtifactType createObjectClassStatisticsArtifact(
            String resourceOid, String resourceName, QName objectClassName, ObjectSetStatisticsType statistics) {
        var object = new SmartIntegrationArtifactType()
                .name("Statistics for %s:%s (%s)".formatted(
                        resourceName, objectClassName.getLocalPart(), statistics.getTimestamp()))
                .scope(new SmartIntegrationArtifactScopeType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectClass(
                                QNameUtil.qualifyIfNeeded(objectClassName, NS_RI)))
                .statistics(statistics);
        setArchetype(object, SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_RESOURCE_OBJECT_CLASS_STATISTICS);
        return object;
    }

    public static SmartIntegrationArtifactType createObjectTypeStatisticsArtifact(
            String resourceOid, String resourceName, ResourceObjectTypeIdentification type, ObjectSetStatisticsType statistics) {
        var object = new SmartIntegrationArtifactType()
                .name("Statistics for %s:%s (%s)".formatted(resourceName, type, statistics.getTimestamp()))
                .scope(new SmartIntegrationArtifactScopeType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectType(type.asBean()))
                .statistics(statistics);
        setArchetype(object, SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_RESOURCE_OBJECT_TYPE_STATISTICS);
        return object;
    }

    public static @NotNull SmartIntegrationArtifactType createFocusTypeStatisticsArtifact(
            QName focusTypeName,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            ObjectSetStatisticsType statistics) {
        var object = new SmartIntegrationArtifactType()
                .name("Focus object statistics for %s on %s/%s (%s)".formatted(
                        focusTypeName, resourceOid, typeIdentification, statistics.getTimestamp()))
                .scope(new SmartIntegrationArtifactScopeType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectType(typeIdentification.asBean())
                        .focusType(focusTypeName))
                .statistics(statistics);
        setArchetype(object, SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_FOCUS_OBJECT_TYPE_STATISTICS);
        return object;
    }

    public static SchemaMatchResultType getObjectTypeSchemaMatchRequired(SmartIntegrationArtifactType holder) {
        return getObjectTypeSchemaMatchRequired(holder.asPrismObject());
    }

    public static SchemaMatchResultType getObjectTypeSchemaMatchRequired(PrismObject<SmartIntegrationArtifactType> holder) {
        return argNonNull(holder.asObjectable().getSchemaMatch(), "No schema match in %s", holder);
    }

    public static SmartIntegrationArtifactType createSchemaMatchArtifact(
            String resourceOid, ResourceObjectTypeIdentification type, SchemaMatchResultType schemaMatch) {
        var object = new SmartIntegrationArtifactType()
                .name("Schema match for %s:%s (%s)".formatted(resourceOid, type, schemaMatch.getTimestamp()))
                .scope(new SmartIntegrationArtifactScopeType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectType(type.asBean()))
                .schemaMatch(schemaMatch);
        setArchetype(object, SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_SCHEMA_MATCH);
        return object;
    }

    /**
     * We create both `archetypeRef` and the assignment to the archetype, because currently we create these objects
     * via repository, not via clockwork. This may change in the future, when we start using model for creating these objects.
     */
    public static void setArchetype(SmartIntegrationArtifactType object, SystemObjectsType archetype) {
        object.archetypeRef(archetype.value(), ArchetypeType.COMPLEX_TYPE);
        object.assignment(new AssignmentType()
                .targetRef(archetype.value(), ArchetypeType.COMPLEX_TYPE));
    }
}
