/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.repo.sqale.MObjectTypeMapping;
import com.evolveum.midpoint.repo.sqale.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ObjectSqlTransformer<S extends ObjectType, Q extends QObject<R>, R extends MObject>
        extends SqaleTransformerBase<S, Q, R> {

    public ObjectSqlTransformer(
            SqlTransformerContext transformerContext,
            QObjectMapping<S, Q, R> mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public S toSchemaObject(R row) {
        throw new UnsupportedOperationException("Use toSchemaObject(Tuple,...)");
    }

    @Override
    public S toSchemaObject(Tuple row, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {

        PrismObject<S> prismObject;
        String serializedForm = new String(row.get(entityPath.fullObject), StandardCharsets.UTF_8);
        try {
            SqlTransformerContext.ParseResult<S> result = transformerContext.parsePrismObject(serializedForm);
            prismObject = result.prismObject;
            if (result.parsingContext.hasWarnings()) {
                logger.warn("Object {} parsed with {} warnings",
                        ObjectTypeUtil.toShortString(prismObject), result.parsingContext.getWarnings().size());
            }
        } catch (SchemaException | RuntimeException | Error e) {
            // This is a serious thing. We have corrupted XML in the repo. This may happen even
            // during system init. We want really loud and detailed error here.
            logger.error("Couldn't parse object {} {}: {}: {}\n{}",
                    mapping.schemaType().getSimpleName(), row.get(entityPath.oid),
                    e.getClass().getName(), e.getMessage(), serializedForm, e);
            throw e;
        }

        return prismObject.asObjectable();
    }

    /**
     * Override this to fill additional row attributes after calling this super version.
     */
    @NotNull
    public R toRowObjectWithoutFullObject(S schemaObject) {
        R row = mapping.newRowObject();

        row.oid = Optional.ofNullable(schemaObject.getOid())
                .map(UUID::fromString)
                .orElse(null);

        // primitive columns common to ObjectType
        PolyStringType name = schemaObject.getName();
        row.nameOrig = name.getOrig();
        row.nameNorm = name.getNorm();

        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            ObjectReferenceType creatorRef = metadata.getCreatorRef();
            if (creatorRef != null) {
                row.creatorRefRelationId = qNameToId(creatorRef.getRelation());
                row.creatorRefTargetOid = UUID.fromString(creatorRef.getOid());
                row.creatorRefTargetType = MObjectTypeMapping.fromSchemaType(
                        transformerContext.qNameToSchemaClass(creatorRef.getType())).code();
            }
            row.createChannelId = qNameToId(metadata.getCreateChannel());
            row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

            ObjectReferenceType modifierRef = metadata.getModifierRef();
            if (modifierRef != null) {
                row.modifierRefRelationId = qNameToId(modifierRef.getRelation());
                row.modifierRefTargetOid = UUID.fromString(modifierRef.getOid());
                row.modifierRefTargetType = MObjectTypeMapping.fromSchemaType(
                        transformerContext.qNameToSchemaClass(modifierRef.getType())).code();
            }
            row.modifyChannelId = qNameToId(metadata.getModifyChannel());
            row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
        }

        ObjectReferenceType tenantRef = schemaObject.getTenantRef();
        if (tenantRef != null) {
            row.tenantRefRelationId = qNameToId(tenantRef.getRelation());
            row.tenantRefTargetOid = UUID.fromString(tenantRef.getOid());
            row.tenantRefTargetType = MObjectTypeMapping.fromSchemaType(
                    transformerContext.qNameToSchemaClass(tenantRef.getType())).code();
        }

        row.lifecycleState = schemaObject.getLifecycleState();
        row.version = SqaleUtils.objectVersionAsInt(schemaObject);

        // TODO extensions

        return row;
    }

    /**
     * Serializes schema object and sets {@link R#fullObject}.
     */
    public void setFullObject(R row, S schemaObject) throws SchemaException {
        row.fullObject = createFullObject(schemaObject);
    }

    public byte[] createFullObject(S schemaObject) throws SchemaException {
        if (schemaObject.getOid() == null || schemaObject.getVersion() == null) {
            throw new IllegalArgumentException(
                    "Serialized object must have assigned OID and version: " + schemaObject);
        }

        return transformerContext.serializer()
                .itemsToSkip(fullObjectItemsToSkip())
                .options(SerializationOptions
                        .createSerializeReferenceNamesForNullOids()
                        .skipIndexOnly(true)
                        .skipTransient(true))
                .serialize(schemaObject.asPrismObject())
                .getBytes(StandardCharsets.UTF_8);
    }

    protected Collection<? extends QName> fullObjectItemsToSkip() {
        // TODO extend later, things like FocusType.F_JPEG_PHOTO, see ObjectUpdater#updateFullObject
        return Collections.emptyList();
    }
}
