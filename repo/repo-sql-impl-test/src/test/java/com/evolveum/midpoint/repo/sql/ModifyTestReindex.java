/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static java.util.Collections.emptySet;
import static org.testng.AssertJUnit.assertEquals;

import javax.xml.namespace.QName;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The same as ModifyTest but with {@link RepoModifyOptions#forceReindex forceReindex} option set.
 * Although this option should do no harm in objects other than certification cases and lookup tables,
 * it is better to check.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTestReindex extends ModifyTest {

    @Override
    protected RepoModifyOptions getModifyOptions() {
        return RepoModifyOptions.createForceReindex();
    }

    @Test
    public void testReindexExtensionItem() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name("unstable")
                .asPrismObject();
        ItemPath UNSTABLE_PATH = ItemPath.create(UserType.F_EXTENSION, "unstable");
        PrismPropertyDefinition<String> unstableDef = user.getDefinition().findPropertyDefinition(UNSTABLE_PATH);
        PrismProperty<String> unstable = unstableDef.instantiate();
        unstable.setRealValue("hi");
        user.addExtensionItem(unstable);

        String oid = repositoryService.addObject(user, null, result);

        // brutal hack -- may stop working in the future!
        unstableDef.toMutable().setIndexed(true);

        repositoryService.modifyObject(UserType.class, oid, emptySet(), getModifyOptions(), result);

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UNSTABLE_PATH).eq("hi")
                .build();
        int count = repositoryService.countObjects(UserType.class, query, null, result);
        assertEquals("Wrong # of objects found", 1, count);
    }

    @Test // MID-5112
    public void testReindexIndexOnlyItem() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name(getTestNameShort())
                .asPrismObject();
        ItemPath INDEX_ONLY_PATH = ItemPath.create(UserType.F_EXTENSION, "indexOnly");
        PrismPropertyDefinition<String> indexOnlyDef = user.getDefinition().findPropertyDefinition(INDEX_ONLY_PATH);
        PrismProperty<String> indexOnlyProperty = indexOnlyDef.instantiate();
        indexOnlyProperty.setRealValue("hi");
        user.addExtensionItem(indexOnlyProperty);

        String oid = repositoryService.addObject(user, null, result);

        UserType userBefore = repositoryService
                .getObject(UserType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();
        displayValue("user before", userBefore.asPrismObject());

        repositoryService.modifyObject(UserType.class, oid, emptySet(), getModifyOptions(), result);

        UserType userAfter = repositoryService
                .getObject(UserType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();
        displayValue("user after", userAfter.asPrismObject());

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(INDEX_ONLY_PATH).eq("hi")
                .build();
        int count = repositoryService.countObjects(UserType.class, query, null, result);
        assertEquals("Wrong # of objects found", 1, count);
    }

    @Test // MID-5112
    public void testReindexPhoto() throws Exception {
        OperationResult result = createOperationResult();

        byte[] PHOTO = new byte[] { 1, 2, 3, 4 };

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name(getTestNameShort())
                .jpegPhoto(PHOTO)
                .asPrismObject();

        String oid = repositoryService.addObject(user, null, result);

        repositoryService.modifyObject(UserType.class, oid, emptySet(), getModifyOptions(), result);

        UserType userAfter = repositoryService
                .getObject(UserType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();

        assertEquals("Missing or wrong photo", PHOTO, userAfter.getJpegPhoto());
    }

    @Test // MID-5112
    public void testReindexTaskResult() throws Exception {
        OperationResult result = createOperationResult();

        OperationResultType taskOperationResult = new OperationResultType().message("Hello there");

        PrismObject<TaskType> task = prismContext.createObjectable(TaskType.class)
                .name(getTestNameShort())
                .result(taskOperationResult)
                .asPrismObject();

        String oid = repositoryService.addObject(task, null, result);

        repositoryService.modifyObject(TaskType.class, oid, emptySet(), getModifyOptions(), result);

        TaskType taskAfter = repositoryService
                .getObject(TaskType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();

        assertEquals("Missing or wrong result", taskOperationResult, taskAfter.getResult());
    }

    @Test // MID-5112
    public void testReindexLookupTableRow() throws Exception {
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType(prismContext)
                .key("key")
                .label("label");
        PrismObject<LookupTableType> table = prismContext.createObjectable(LookupTableType.class)
                .name(getTestNameShort())
                .row(row)
                .asPrismObject();

        String oid = repositoryService.addObject(table, null, result);

        repositoryService.modifyObject(LookupTableType.class, oid, emptySet(), getModifyOptions(), result);

        LookupTableType tableAfter = repositoryService
                .getObject(LookupTableType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();

        assertEquals("Missing row", 1, tableAfter.getRow().size());
        assertEquals("Wrong row", row, tableAfter.getRow().get(0));
    }

    @Test // MID-5112
    public void testReindexCertificationCampaignCase() throws Exception {
        OperationResult result = createOperationResult();

        AccessCertificationCaseType aCase = new AccessCertificationCaseType(prismContext)
                .stageNumber(1);
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.createObjectable(AccessCertificationCampaignType.class)
                .name(getTestNameShort())
                .stageNumber(1)
                ._case(aCase)
                .asPrismObject();

        String oid = repositoryService.addObject(campaign, null, result);

        repositoryService.modifyObject(AccessCertificationCampaignType.class, oid, emptySet(), getModifyOptions(), result);

        AccessCertificationCampaignType campaignAfter = repositoryService
                .getObject(AccessCertificationCampaignType.class, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result)
                .asObjectable();

        assertEquals("Missing case", 1, campaignAfter.getCase().size());
        assertEquals("Wrong case", aCase, campaignAfter.getCase().get(0));
    }

    /**
     * MID-5128
     */
    @Test
    public void testReindexShadow() throws Exception {
        OperationResult result = createOperationResult();

        String APPROVER_OID = "9123090439201432";
        PrismObject<ShadowType> shadow = prismContext.createObjectable(ShadowType.class)
                .name("unstable")
                .beginMetadata()
                .modifyApproverRef(APPROVER_OID, UserType.COMPLEX_TYPE)
                .<ShadowType>end()
                .asPrismObject();
        MutablePrismPropertyDefinition<String> def = prismContext.definitionFactory().createPropertyDefinition(new QName("http://temp/", "attr1"), DOMUtil.XSD_STRING);
        def.setIndexed(true);
        PrismProperty<String> attribute = def.instantiate();
        attribute.addRealValue("value");
        shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES).add(attribute);

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).ref(APPROVER_OID, UserType.COMPLEX_TYPE)
                .build();

        // add shadow and check metadata search

        String oid = repositoryService.addObject(shadow, null, result);

        int count = repositoryService.countObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of objects found (after creation)", 1, count);

        // break metadata in repo

        EntityManager em = factory.createEntityManager();

        System.out.println("definitions: " + em.createQuery("from RExtItem").getResultList());
        System.out.println("ext values: " + em.createQuery("from ROExtString").getResultList());

        em.getTransaction().begin();
        Query updateQuery = em.createQuery(
                "update com.evolveum.midpoint.repo.sql.data.common.RObjectReference"
                        + " set targetType = null where ownerOid = '" + oid + "'");
        System.out.println("records modified = " + updateQuery.executeUpdate());
        em.getTransaction().commit();
        em.close();

        // verify search is broken

        count = repositoryService.countObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of objects found (after zeroing the type)", 0, count);

        // reindex

        repositoryService.modifyObject(ShadowType.class, oid, emptySet(), getModifyOptions(), result);

        // verify search is OK

        count = repositoryService.countObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of objects found (after reindexing)", 1, count);
    }
}
