/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.metadata;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType.F_STORAGE;

import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;

public abstract class QValueMetadataMapping<OR, M extends MValueMetadata, Q extends QValueMetadata<M, OR>>
        extends QContainerMapping<ValueMetadataType, Q, M, OR> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected QValueMetadataMapping(String tableName,String  aliasName, Class<Q> queryType, @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, aliasName,
                ValueMetadataType.class, queryType, repositoryContext);

        addNestedMapping(F_STORAGE, StorageMetadataType.class)
                .addRefMapping(StorageMetadataType.F_CREATOR_REF,
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(StorageMetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(StorageMetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addRefMapping(StorageMetadataType.F_MODIFIER_REF,
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(StorageMetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(StorageMetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp));
    }

    @Override
    protected abstract Q newAliasInstance(String alias);

    @Override
    public abstract M newRowObject();

    @Override
    public abstract M newRowObject(OR ownerRow);

    @Override
    public abstract M insert(ValueMetadataType assignment, OR ownerRow, JdbcSession jdbcSession);


    @Override
    public M initRowObject(ValueMetadataType schemaObject, OR ownerRow) {
        M row =  super.initRowObject(schemaObject, ownerRow);
        var storage = schemaObject.getStorage();
        if (storage != null) {
            setReference(storage.getCreatorRef(),
                    o -> row.creatorRefTargetOid = o,
                    t -> row.creatorRefTargetType = t,
                    r -> row.creatorRefRelationId = r);
            row.createChannelId = processCacheableUri(storage.getCreateChannel());
            row.createTimestamp = MiscUtil.asInstant(storage.getCreateTimestamp());

            setReference(storage.getModifierRef(),
                    o -> row.modifierRefTargetOid = o,
                    t -> row.modifierRefTargetType = t,
                    r -> row.modifierRefRelationId = r);
            row.modifyChannelId = processCacheableUri(storage.getModifyChannel());
            row.modifyTimestamp = MiscUtil.asInstant(storage.getModifyTimestamp());
        }
        return row;
    }


}
