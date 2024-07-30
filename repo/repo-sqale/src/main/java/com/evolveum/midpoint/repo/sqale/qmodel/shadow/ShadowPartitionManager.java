package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShadowPartitionManager {
    public static final String DEFAULT_PARTITION = "m_shadow_default";
    private static final String TABLE_PREFIX = "m_shadow_";
    private static final String DEFAULT_SUFFIX = "_default";
    private final SqaleRepoContext repoContext;

    Map<UUID, ResourceTable> resourceTable;

    public ShadowPartitionManager(SqaleRepoContext repositoryContext) {
        this.repoContext = repositoryContext;
    }

    public void ensurePartitionExists(MShadow row, JdbcSession jdbcSession) {
        ResourceTable resource = getOrCreateResourceView(row.resourceRefTargetOid, jdbcSession);
        resource.getOrCreateObjectClassTable(row.objectClassId, jdbcSession);
        resource.ensureAttached();
    }

    private ResourceTable getOrCreateResourceView(UUID resourceRefTargetOid,JdbcSession jdbcSession) {
        var view = resourceTableLoaded().get(resourceRefTargetOid);
        if (view != null) {
            return view;
        }
        return loadOrCreateResourceView(resourceRefTargetOid, jdbcSession);
    }

    private ResourceTable loadOrCreateResourceView(UUID resourceOid, JdbcSession jdbcSession) {
        var partitionDef = alias();
        var dbView = jdbcSession.newQuery().from(partitionDef)
                .select(partitionDef)
                .where(partitionViewPredicate(resourceOid, partitionDef))
                .fetchOne();
        if (dbView != null) {
            return resourceViewFromDb(dbView);
        }
        return createResourceView(resourceOid, jdbcSession);

    }

    private Predicate partitionViewPredicate(UUID resourceOid, QShadowPartitionRef partitionDef) {
        return partitionDef.resourceOid.eq(resourceOid)
                .and(partitionDef.objectClassId.isNull())
                .and(partitionDef.partition.isFalse());
    }

    private ResourceTable createResourceView(UUID resourceOid, JdbcSession jdbcSession) {
        var resourceTable = new MShadowPartitionDef();
        resourceTable.resourceOid = resourceOid;
        resourceTable.objectClassId = null;

        // Resource is table, not view
        resourceTable.partition = false;
        resourceTable.table = TABLE_PREFIX + tableOid(resourceOid);
        resourceTable.attached = false;


        var defaultPartition = new MShadowPartitionDef();
        defaultPartition.resourceOid = resourceOid;
        defaultPartition.objectClassId = null;

        // Resource is table, not view
        defaultPartition.partition = true;
        defaultPartition.table = TABLE_PREFIX + tableOid(resourceOid) + DEFAULT_SUFFIX;
        defaultPartition.attached = false;

        jdbcSession.newInsert(alias()).populate(resourceTable).execute();
        jdbcSession.newInsert(alias()).populate(defaultPartition).execute();

        var partitionDef = alias();

        jdbcSession.newUpdate(partitionDef)
                .set(partitionDef.attached, true)
                .where(partitionViewPredicate(resourceOid, partitionDef)).execute();

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
        return new HashMap<>();
    }

    public ResourceTable getResourceTable(UUID newResourceOid) {
        return resourceTableLoaded().get(newResourceOid);
    }

    public void createMissingPartitions(OperationResult result) {

        try (var session = repoContext.newJdbcSession()) {
            var s = new QShadow("s", FlexibleRelationalPathBase.DEFAULT_SCHEMA_NAME, DEFAULT_PARTITION);
            List<Tuple> existingCombinations = session.newQuery().from(s)
                    .select(s.resourceRefTargetOid, s.objectClassId)
                    .groupBy(s.resourceRefTargetOid, s.objectClassId)
                    .orderBy(s.resourceRefTargetOid.asc(), s.objectClassId.asc())
                    .fetch();

           for (var combo : existingCombinations) {
               var coordinates = new MShadow();
               coordinates.resourceRefTargetOid = combo.get(s.resourceRefTargetOid);
               coordinates.objectClassId = combo.get(s.objectClassId);
               ensurePartitionExists(coordinates, session);
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
                jdbcSession.newInsert(defAlias)
                        .populate(dbRow)
                        .execute();
            }


            var ret = new ObjectClassPartition(dbRow);
            objectClassTable.put(objectClassId, ret);
            return ret;
        }

        public void ensureAttached() {

        }

        public String getTableName() {
            return row.table;
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
