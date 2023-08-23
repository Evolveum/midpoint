/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.SinglePathItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.TypeQNameItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetypeMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAffectedObjectsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicObjectSetType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.querydsl.core.types.dsl.EnumPath;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType.F_TYPE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAffectedObjectsType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.BasicObjectSetType.*;


public class QAffectedObjectsMapping extends QContainerMapping<ActivityAffectedObjectsType, QAffectedObjects, MAffectedObjects, MTask> {

    public static final String DEFAULT_ALIAS_NAME = "tao";

    private static QAffectedObjectsMapping instance;


    // Explanation in class Javadoc for SqaleTableMapping
    public static QAffectedObjectsMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAffectedObjectsMapping(repositoryContext);
        }
        return instance;
    }

    protected QAffectedObjectsMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAffectedObjects.TABLE_NAME, DEFAULT_ALIAS_NAME, ActivityAffectedObjectsType.class, QAffectedObjects.class, repositoryContext);

        addItemMapping(F_ACTIVITY_TYPE, uriMapper(q -> q.activityId));
        addItemMapping(F_EXECUTION_MODE, enumMapper(q -> q.executionMode));
        addItemMapping(F_PREDEFINED_CONFIGURATION_TO_USE, enumMapper(q -> q.predefinedConfigurationToUse));
        Function<QAffectedObjects, EnumPath<MObjectType>> objectTypePath = q -> q.type;
        addNestedMapping(F_OBJECTS, BasicObjectSetType.class)
                .addItemMapping(F_TYPE, new SqaleItemSqlMapper<>(
                        ctx -> new TypeQNameItemFilterProcessor(ctx, objectTypePath),
                        ctx -> new SinglePathItemDeltaProcessor<>(ctx, objectTypePath),
                        objectTypePath
                ))
                .addRefMapping(F_ARCHETYPE_REF,
                        q -> q.archetypeRefTargetOid,
                        q -> q.archetypeRefTargetType,
                        q -> q.archetypeRefRelationId,
                        QArchetypeMapping::getArchetypeMapping);
        addNestedMapping(F_RESOURCE_OBJECTS, BasicResourceObjectSetType.class)
                .addItemMapping(F_OBJECTCLASS, uriMapper(q -> q.objectClassId))
                .addRefMapping(F_RESOURCE_REF,
                        q -> q.resourceRefTargetOid,
                        q -> q.resourceRefTargetType,
                        q -> q.resourceRefRelationId,
                        QResourceMapping::get)
                .addItemMapping(F_INTENT, stringMapper(q -> q.intent))
                .addItemMapping(F_KIND, enumMapper(q -> q.kind));

    }

    public static QAffectedObjectsMapping get() {
        return instance;
    }

    @Override
    public MAffectedObjects newRowObject() {
        return new MAffectedObjects();
    }

    @Override
    public MAffectedObjects newRowObject(MTask ownerRow) {
        var row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    protected QAffectedObjects newAliasInstance(String alias) {
        return new QAffectedObjects(alias);
    }

    @Override
    public MAffectedObjects insert(ActivityAffectedObjectsType activity, MTask ownerRow, JdbcSession jdbcSession) throws SchemaException {
        var row = initRowObject(activity, ownerRow);

        row.activityId = processCacheableUri(activity.getActivityType());

        row.executionMode = activity.getExecutionMode();
        row.predefinedConfigurationToUse = activity.getPredefinedConfigurationToUse();

        var object = activity.getObjects();
        if (object != null) {
            setReference(object.getArchetypeRef(),
                    o -> row.archetypeRefTargetOid = o,
                    t -> row.archetypeRefTargetType = t,
                    r -> row.archetypeRefRelationId = r
            );
            if (object.getType() != null) {
                row.type = MObjectType.fromTypeQName(object.getType());
            }
        }
        var resource = activity.getResourceObjects();
        if (resource != null) {
            setReference(resource.getResourceRef(),
                    o -> row.resourceRefTargetOid = o ,
                    t -> row.resourceRefTargetType = t,
                    r -> row.resourceRefRelationId = r
            );
            row.kind = resource.getKind();
            row.intent = resource.getIntent();
            row.objectClassId = processCacheableUri(resource.getObjectclass());
        }
        insert(row, jdbcSession);
        return  row;
    }
}
