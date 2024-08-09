package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;


import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;

import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadowMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.ShadowPartitionManager;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShadowPartitioningTest extends SqaleRepoBaseTest {

    private static final int NON_MIGRATED_RESOURCE_COUNT = 5;
    private static final int SHADOWS_PER_RESOURCE_OBJECTCLASS = 100;
    private static final List<QName> OBJECT_CLASSES = ImmutableList.of(
            SchemaConstants.RI_ACCOUNT_OBJECT_CLASS,
            SchemaConstants.RI_GROUP_OBJECT_CLASS);
    private ShadowPartitionManager partitionManager;
    private ArrayList<UUID> resourcesOids;
    private QShadowMapping shadowMapping;

    @BeforeClass
    public void initObjects() throws Exception {
        super.initDatabase();
        this.shadowMapping = (QShadowMapping) ((QueryTableMapping) sqlRepoContext.getMappingBySchemaType(ShadowType.class));
        shadowMapping.getPartitionManager().setPartitionCreationOnAdd(false);
        var result = createOperationResult();
        this.partitionManager = shadowMapping.getPartitionManager();

        this.resourcesOids = new ArrayList<>();
        for (int i = 0; i < NON_MIGRATED_RESOURCE_COUNT; i++) {
            resourcesOids.add(UUID.randomUUID());
        }
        for (var resource : resourcesOids) {
            populateResourceWithShadows(resource, result);
        }
    }

    private void populateResourceWithShadows(UUID resource, OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
        for (var objectClass : OBJECT_CLASSES) {
            for (int i = 0; i < SHADOWS_PER_RESOURCE_OBJECTCLASS; i++) {
                repositoryService.addObject(new ShadowType()
                                .name(Strings.lenientFormat("%s:%s:%s",resource.toString(), objectClass.getLocalPart(), i))
                                .objectClass(objectClass)
                                .resourceRef(resource.toString(), ResourceType.COMPLEX_TYPE)
                                .asPrismObject(),
                        null, result
                );
            }
        }
    }

    private QShadow shadowPartitionAlias(String tableName) {
        return new QShadow("s", FlexibleRelationalPathBase.DEFAULT_SCHEMA_NAME, tableName);
    }

    @Test
    public void test100AllShadowsInDefaultPartition() {
        var shadowsCount = countShadowsIn(ShadowPartitionManager.DEFAULT_PARTITION);
        assertThat(shadowsCount).isEqualTo(resourcesOids.size() * OBJECT_CLASSES.size() * SHADOWS_PER_RESOURCE_OBJECTCLASS);
    }

    private long countShadowsIn(String partition) {
        var s = shadowPartitionAlias(partition);
        try (var session = sqlRepoContext.newJdbcSession()) {
            return session.newQuery().from(s).fetchCount();
        }
    }

    @Test
    public void  test200PartitioningEnabledNewResourceAdded() throws SchemaException, ObjectAlreadyExistsException {
        var result = createOperationResult();
        when("Partitioning is enabled");
        shadowMapping.getPartitionManager().setPartitionCreationOnAdd(true);

        when("new resource shadows are discovered");
        var newResourceOid = UUID.randomUUID();
        resourcesOids.add(newResourceOid);
        populateResourceWithShadows(newResourceOid, result);
        then("new partitions are created and contains shadows from new resource");
        var resourceTableInfo = partitionManager.getResourceTable(newResourceOid);
        assertThat(countShadowsIn(resourceTableInfo.getTableName())).isEqualTo(SHADOWS_PER_RESOURCE_OBJECTCLASS * OBJECT_CLASSES.size());
    }

    @Test
    public void test300CreateMissingPartitions() throws SchemaException, ObjectAlreadyExistsException {
        var result = createOperationResult();
        repositoryService.createPartitionsForExistingData(result);

        for (var resource : resourcesOids) {
            var resourceTableInfo = partitionManager.getResourceTable(resource);
            assertThat(countShadowsIn(resourceTableInfo.getTableName())).isEqualTo(SHADOWS_PER_RESOURCE_OBJECTCLASS * OBJECT_CLASSES.size());
        }
        assertThat(countShadowsIn(QShadow.TABLE_NAME)).isEqualTo(resourcesOids.size() * SHADOWS_PER_RESOURCE_OBJECTCLASS * OBJECT_CLASSES.size());
    }


}
