/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.PartitionManager;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeAdminGuiConfigurationType;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowPartitionManager implements PartitionManager<MShadow> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowPartitionManager.class);

    public static final String DEFAULT_PARTITION = "m_shadow_default";
    private static final String TABLE_PREFIX = "m_shadow_";
    private static final String DEFAULT_SUFFIX = "_default";
    private final SqaleRepoContext repoContext;

    Map<UUID, ResourceTable> resourceTable;
    private boolean partitionCreationOnAdd;
    private boolean createDefaultPartitions;


    public ShadowPartitionManager(SqaleRepoContext repositoryContext) {
        this.repoContext = repositoryContext;
    }

    @Override
    public void setPartitionCreationOnAdd(boolean value) {
        this.partitionCreationOnAdd = value;
    }

    public boolean isPartitionCreationOnAdd() {
        return partitionCreationOnAdd;
    }

    @Override
    public synchronized void ensurePartitionExists(MShadow row, JdbcSession jdbcSession) {
        Preconditions.checkArgument(row.objectClassId != null, "objectClass needs to be present in shadow");
        Preconditions.checkArgument(row.resourceRefTargetOid != null, "resourceRef needs to be present in shadow");
        ResourceTable resource = getOrCreateResourceView(row.resourceRefTargetOid, jdbcSession, true);
        resource.getOrCreateObjectClassTable(row.objectClassId, jdbcSession);
        resource.attach(jdbcSession);
    }

    private ResourceTable getOrCreateResourceView(UUID resourceRefTargetOid,JdbcSession jdbcSession, boolean createDefault) {
        var view = resourceTableLoaded().get(resourceRefTargetOid);
        if (view != null) {
            return view;
        }
        return loadOrCreateResourceView(resourceRefTargetOid, jdbcSession, createDefault);
    }

    private ResourceTable loadOrCreateResourceView(UUID resourceOid, JdbcSession jdbcSession, boolean createDefault) {
        var partitionDef = alias();
        var dbView = jdbcSession.newQuery().from(partitionDef)
                .select(partitionDef)
                .where(partitionViewPredicate(resourceOid, partitionDef))
                .fetchOne();
        if (dbView != null) {
            return resourceViewFromDb(dbView);
        }
        return createResourceView(resourceOid, jdbcSession, createDefault);

    }

    private Predicate partitionViewPredicate(UUID resourceOid, QShadowPartitionRef partitionDef) {
        return partitionDef.resourceOid.eq(resourceOid)
                .and(partitionDef.objectClassId.isNull())
                .and(partitionDef.partition.isFalse());
    }

    private ResourceTable createResourceView(UUID resourceOid, JdbcSession jdbcSession, boolean createDefault) {
        LOGGER.trace("Creating resource view for {}, createDefault={}", resourceOid, createDefault);

        var resourceTable = new MShadowPartitionDef();
        resourceTable.resourceOid = resourceOid;
        resourceTable.objectClassId = null;

        // Resource is table, not view
        resourceTable.partition = false;
        resourceTable.table = TABLE_PREFIX + tableOid(resourceOid);
        resourceTable.attached = false;
        jdbcSession.newInsert(alias()).populate(resourceTable).execute();
        if (createDefault) {
            var defaultPartition = new MShadowPartitionDef();
            defaultPartition.resourceOid = resourceOid;
            defaultPartition.objectClassId = null;
            // Defualt Partition is actual partition
            defaultPartition.partition = true;
            defaultPartition.table = TABLE_PREFIX + tableOid(resourceOid) + DEFAULT_SUFFIX;
            defaultPartition.attached = false;
            LOGGER.trace("Creating default partition for {}, table {}", defaultPartition.resourceOid, defaultPartition.table);
            jdbcSession.newInsert(alias()).populate(defaultPartition).execute();
        }

        return resourceViewFromDb(resourceTable);
    }

    private String tableOid(UUID resourceOid) {
        return resourceOid.toString().replace('-','_');
    }

    private ResourceTable resourceViewFromDb(MShadowPartitionDef dbView) {
        var view = new ResourceTable(dbView);
        resourceTableLoaded().put(dbView.resourceOid, view);
        // FIXME: SHould we fetch  default table details here?
        return view;
    }

    private QShadowPartitionRef alias() {
        return new QShadowPartitionRef("d");
    }

    private Map<UUID, ResourceTable> resourceTableLoaded() {
        if (resourceTable == null) {
            resourceTable = loadResourceTable();
        }
        return resourceTable;
    }

    private Map<UUID, ResourceTable> loadResourceTable() {
        // FIXME: We can select and load table based
        return new ConcurrentHashMap<>();
    }

    @VisibleForTesting
    public ResourceTable getResourceTable(UUID newResourceOid) {
        return resourceTableLoaded().get(newResourceOid);
    }

    @Override
    public synchronized void createMissingPartitions(OperationResult parentResult) {
        LOGGER.trace("Creating missing partitions");

        try (var session = repoContext.newJdbcSession()) {
            var s = new QShadow("s", FlexibleRelationalPathBase.DEFAULT_SCHEMA_NAME, DEFAULT_PARTITION);
            List<Tuple> existingCombinations = session.newQuery().from(s)
                    .select(s.resourceRefTargetOid, s.objectClassId)
                    .groupBy(s.resourceRefTargetOid, s.objectClassId)
                    .orderBy(s.resourceRefTargetOid.asc(), s.objectClassId.asc())
                    .fetch();

            Map<UUID,ResourceTable> toAttach = new HashMap<>();
            for (var combo : existingCombinations) {
               var row = new MShadow();
               row.resourceRefTargetOid = combo.get(s.resourceRefTargetOid);
               row.objectClassId = combo.get(s.objectClassId);

               var resource = toAttach.get(row.resourceRefTargetOid);
               if (resource == null) {
                   resource = getOrCreateResourceView(row.resourceRefTargetOid, session, false);
                   toAttach.put(resource.row.resourceOid, resource);
               }
               resource.getOrCreateObjectClassTable(row.objectClassId, session);
            }
            for (var resource : toAttach.values()) {
                LOGGER.info("Attaching resource {}", resource.row.resourceOid);

                resource.attach(session);
            }

            session.commit();
        }
    }

    @VisibleForTesting
    public class ResourceTable {
        private final MShadowPartitionDef row;
        DefaultPartition defaultTable;
        Map<Integer, ObjectClassPartition> objectClassTable = new HashMap<>();

        public ResourceTable(MShadowPartitionDef dbView) {
            this.row = dbView;
        }

        public ObjectClassPartition getOrCreateObjectClassTable(Integer objectClassId, JdbcSession jdbcSession) {
            var table = objectClassTable.get(objectClassId);
            if (table != null) {
                return table;
            }

            var defAlias = alias();
            var dbRow = jdbcSession.newQuery().from(defAlias)
                    .select(defAlias)
                    .where(objectClassTablePredicate(row.resourceOid, objectClassId,defAlias)).fetchOne();
            if (dbRow == null) {
                dbRow = new MShadowPartitionDef();
                dbRow.resourceOid = row.resourceOid;
                dbRow.objectClassId = objectClassId;
                dbRow.partition = true;
                dbRow.attached = true;
                dbRow.table = TABLE_PREFIX + tableOid(row.resourceOid) + "_" + objectClassId;
                LOGGER.trace("Creating object class table {}", dbRow.table);

                jdbcSession.newInsert(defAlias)
                        .populate(dbRow)
                        .execute();
            }


            var ret = new ObjectClassPartition(dbRow);
            objectClassTable.put(objectClassId, ret);
            return ret;
        }

        public String getTableName() {
            return row.table;
        }

        public void attach(JdbcSession session) {
            var partitionDef = alias();

            LOGGER.trace("Attaching resource {}, table {}", row.resourceOid, row.table);
            session.newUpdate(partitionDef)
                    .set(partitionDef.attached, true)
                    .where(partitionViewPredicate(row.resourceOid, partitionDef)).execute();
        }

        public void createDefaultTable() {

        }
    }

    private Predicate objectClassTablePredicate(UUID resourceOid, Integer objectClassId, QShadowPartitionRef def) {
        return def.resourceOid.eq(resourceOid).and(def.objectClassId.eq(objectClassId)).and(def.partition.isTrue());
    }

    private class DefaultPartition {

    }

    private class ObjectClassPartition {

        public ObjectClassPartition(MShadowPartitionDef dbRow) {

        }
    }
}
