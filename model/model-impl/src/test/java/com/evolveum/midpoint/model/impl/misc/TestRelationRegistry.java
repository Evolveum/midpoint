/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.misc;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.evolveum.midpoint.util.QNameUtil.unqualify;
import static java.util.Collections.singleton;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRelationRegistry extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");
    private static final File SYSCONFIG_ADDED_CUSTOM_RELATIONS_FILE = new File(TEST_DIR, "sysconfig-added-custom-relations.xml");
    private static final File SYSCONFIG_REPLACED_ALL_RELATIONS_FILE = new File(TEST_DIR, "sysconfig-replaced-all-relations.xml");

    private static final String TEST_NS = "http://test";
    private static final QName TEST_KINDER_MANAGER = new QName(TEST_NS, "kinderManager");
    private static final QName TEST_EXTRA = new QName(TEST_NS, "extra");
    private static final QName TEST_APPROVER = new QName(TEST_NS, "approver");

    @Autowired protected RelationRegistry relationRegistry;

    @Test
    public void test100DefaultRelations() {
        final String TEST_NAME = "test100DefaultRelations";

        assertEquals("Wrong # of default relations", RelationTypes.values().length, relationRegistry.getRelationDefinitions().size());

        RelationDefinitionType orgDefaultDef = relationRegistry.getRelationDefinition(SchemaConstants.ORG_DEFAULT);
        RelationDefinitionType defaultDef = relationRegistry.getRelationDefinition(unqualify(SchemaConstants.ORG_DEFAULT));
        RelationDefinitionType nullDef = relationRegistry.getRelationDefinition(null);
        assertNotNull("No definition for null relation", nullDef);
        assertEquals("null and 'org:default' definitions differ", nullDef, orgDefaultDef);
        assertEquals("null and 'default' definitions differ", nullDef, defaultDef);

        assertTrue("'org:manager' is not of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_MANAGER));
        assertTrue("'manager' is not of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_MANAGER)));
        assertFalse("'org:approver' is of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_APPROVER));
        assertFalse("'approver' is of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_APPROVER)));
        assertFalse("'org:default' is of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_DEFAULT));
        assertFalse("'default' is of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertFalse("'null' is of MANAGER kind", relationRegistry.isManager(null));

        assertTrue("'org:default' is not of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not of MEMBER kind", relationRegistry.isMember(null));
        assertTrue("'org:manager' is not of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_MANAGER));
        assertTrue("'manager' is not of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_MANAGER)));
        assertFalse("'org:approver' is of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_APPROVER));
        assertFalse("'approver' is of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_APPROVER)));

        assertEquals("Wrong default relation", SchemaConstants.ORG_DEFAULT, relationRegistry.getDefaultRelation());
        assertEquals("Wrong default MEMBER relation", SchemaConstants.ORG_DEFAULT, relationRegistry.getDefaultRelationFor(
                RelationKindType.MEMBER));
        assertEquals("Wrong default MANAGER relation", SchemaConstants.ORG_MANAGER, relationRegistry.getDefaultRelationFor(
                RelationKindType.MANAGER));
        assertEquals("Wrong default META relation", SchemaConstants.ORG_META, relationRegistry.getDefaultRelationFor(
                RelationKindType.META));

        Set<QName> ALIASES_FOR_DEFAULT = new HashSet<>(Arrays.asList(SchemaConstants.ORG_DEFAULT, unqualify(SchemaConstants.ORG_DEFAULT), null));
        assertEquals("Wrong aliases for 'org:default'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(SchemaConstants.ORG_DEFAULT)));
        assertEquals("Wrong aliases for 'default'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(unqualify(SchemaConstants.ORG_DEFAULT))));
        assertEquals("Wrong aliases for 'null'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(null)));

        Set<QName> ALIASES_FOR_MANAGER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_MANAGER, unqualify(SchemaConstants.ORG_MANAGER)));
        assertEquals("Wrong aliases for 'org:manager'", ALIASES_FOR_MANAGER, new HashSet<>(relationRegistry.getAliases(SchemaConstants.ORG_MANAGER)));
        assertEquals("Wrong aliases for 'manager'", ALIASES_FOR_MANAGER, new HashSet<>(relationRegistry.getAliases(unqualify(SchemaConstants.ORG_MANAGER))));

        Set<QName> RELATIONS_FOR_MEMBER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_DEFAULT, SchemaConstants.ORG_MANAGER));
        assertEquals("Wrong relations for MEMBER kind", RELATIONS_FOR_MEMBER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MEMBER)));

        Set<QName> RELATIONS_FOR_MANAGER = new HashSet<>(singleton(SchemaConstants.ORG_MANAGER));
        assertEquals("Wrong relations for MANAGER kind", RELATIONS_FOR_MANAGER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MANAGER)));

        assertTrue("'org:default' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not processed on login", relationRegistry.isProcessedOnLogin(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not processed on login", relationRegistry.isProcessedOnLogin(null));
        assertTrue("'org:manager' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not processed on recompute", relationRegistry.isProcessedOnRecompute(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not processed on recompute", relationRegistry.isProcessedOnRecompute(null));
        assertTrue("'org:manager' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(null));
        assertTrue("'org:manager' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_MANAGER));
        assertFalse("'org:meta' is stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not automatically matched", relationRegistry.isAutomaticallyMatched(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not automatically matched", relationRegistry.isAutomaticallyMatched(null));
        assertTrue("'org:manager' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_APPROVER));
    }

    @Test
    public void test110AddedCustomRelations() throws SchemaException, IOException {
        final String TEST_NAME = "test110AddedCustomRelations";

        PrismObject<SystemConfigurationType> sysconfigObject = prismContext.parseObject(SYSCONFIG_ADDED_CUSTOM_RELATIONS_FILE);
        relationRegistry.applyRelationsConfiguration(sysconfigObject.asObjectable());

        assertEquals("Wrong # of relations", RelationTypes.values().length + 3, relationRegistry.getRelationDefinitions().size());

        RelationDefinitionType orgDefaultDef = relationRegistry.getRelationDefinition(SchemaConstants.ORG_DEFAULT);
        RelationDefinitionType defaultDef = relationRegistry.getRelationDefinition(unqualify(SchemaConstants.ORG_DEFAULT));
        RelationDefinitionType nullDef = relationRegistry.getRelationDefinition(null);
        assertNotNull("No definition for null relation", nullDef);
        assertEquals("null and 'org:default' definitions differ", nullDef, orgDefaultDef);
        assertEquals("null and 'default' definitions differ", nullDef, defaultDef);

        RelationDefinitionType testKinderManagerDef = relationRegistry.getRelationDefinition(TEST_KINDER_MANAGER);
        RelationDefinitionType kinderManagerDef = relationRegistry.getRelationDefinition(unqualify(TEST_KINDER_MANAGER));
        RelationDefinitionType testExtraDef = relationRegistry.getRelationDefinition(TEST_EXTRA);
        RelationDefinitionType extraDef = relationRegistry.getRelationDefinition(unqualify(TEST_EXTRA));
        RelationDefinitionType testApproverDef = relationRegistry.getRelationDefinition(TEST_APPROVER);
        RelationDefinitionType approverDef = relationRegistry.getRelationDefinition(unqualify(TEST_APPROVER));
        assertNotNull("No definition for 'test:kinderManager' relation", testKinderManagerDef);
        assertEquals("'test:kinderManager' and 'kinderManager' definitions differ", testKinderManagerDef, kinderManagerDef);
        assertNotNull("No definition for 'test:extra' relation", testExtraDef);
        assertEquals("'test:extra' and 'extra' definitions differ", testExtraDef, extraDef);
        assertNotNull("No definition for 'test:approverDef' relation", testApproverDef);
        assertNotEquals("'test:approver' and 'approver' definitions are the same!", testApproverDef, approverDef);

        assertTrue("'org:manager' is not of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_MANAGER));
        assertTrue("'manager' is not of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_MANAGER)));
        assertTrue("'test:kinderManager' is not of MANAGER kind", relationRegistry.isManager(TEST_KINDER_MANAGER));
        assertTrue("'kinderManager' is not of MANAGER kind", relationRegistry.isManager(unqualify(TEST_KINDER_MANAGER)));
        assertFalse("'org:approver' is of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_APPROVER));
        assertFalse("'approver' is of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_APPROVER)));
        assertFalse("'org:default' is of MANAGER kind", relationRegistry.isManager(SchemaConstants.ORG_DEFAULT));
        assertFalse("'default' is of MANAGER kind", relationRegistry.isManager(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertFalse("'null' is of MANAGER kind", relationRegistry.isManager(null));
        assertFalse("'test:extra' is of MANAGER kind", relationRegistry.isManager(TEST_EXTRA));
        assertFalse("'test:approver' is of MANAGER kind", relationRegistry.isManager(TEST_APPROVER));

        assertTrue("'org:default' is not of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not of MEMBER kind", relationRegistry.isMember(null));
        assertTrue("'org:manager' is not of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_MANAGER));
        assertTrue("'manager' is not of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_MANAGER)));
        assertFalse("'org:approver' is of MEMBER kind", relationRegistry.isMember(SchemaConstants.ORG_APPROVER));
        assertFalse("'approver' is of MEMBER kind", relationRegistry.isMember(unqualify(SchemaConstants.ORG_APPROVER)));
        assertFalse("'test:kinderManager' is of MEMBER kind", relationRegistry.isMember(TEST_KINDER_MANAGER));
        assertFalse("'kinderManager' is of MEMBER kind", relationRegistry.isMember(unqualify(TEST_KINDER_MANAGER)));
        assertFalse("'test:extra' is of MEMBER kind", relationRegistry.isMember(TEST_EXTRA));
        assertFalse("'test:approver' is of MEMBER kind", relationRegistry.isMember(TEST_APPROVER));

        assertFalse("'test:approver' is of APPROVER kind", relationRegistry.isApprover(TEST_APPROVER));
        assertTrue("'approver' is not of APPROVER kind", relationRegistry.isApprover(unqualify(TEST_APPROVER)));

        assertEquals("Wrong default relation", SchemaConstants.ORG_DEFAULT, relationRegistry.getDefaultRelation());
        assertEquals("Wrong default MEMBER relation", SchemaConstants.ORG_DEFAULT, relationRegistry.getDefaultRelationFor(RelationKindType.MEMBER));
        assertEquals("Wrong default MANAGER relation", TEST_KINDER_MANAGER, relationRegistry.getDefaultRelationFor(RelationKindType.MANAGER));
        assertEquals("Wrong default META relation", SchemaConstants.ORG_META, relationRegistry.getDefaultRelationFor(RelationKindType.META));

        Set<QName> ALIASES_FOR_DEFAULT = new HashSet<>(Arrays.asList(SchemaConstants.ORG_DEFAULT, unqualify(SchemaConstants.ORG_DEFAULT), null));
        assertEquals("Wrong aliases for 'org:default'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(SchemaConstants.ORG_DEFAULT)));
        assertEquals("Wrong aliases for 'default'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(unqualify(SchemaConstants.ORG_DEFAULT))));
        assertEquals("Wrong aliases for 'null'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(null)));

        Set<QName> ALIASES_FOR_MANAGER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_MANAGER, unqualify(SchemaConstants.ORG_MANAGER)));
        assertEquals("Wrong aliases for 'org:manager'", ALIASES_FOR_MANAGER, new HashSet<>(relationRegistry.getAliases(SchemaConstants.ORG_MANAGER)));
        assertEquals("Wrong aliases for 'manager'", ALIASES_FOR_MANAGER, new HashSet<>(relationRegistry.getAliases(unqualify(SchemaConstants.ORG_MANAGER))));

        Set<QName> ALIASES_FOR_KINDER_MANAGER = new HashSet<>(Arrays.asList(TEST_KINDER_MANAGER, unqualify(TEST_KINDER_MANAGER)));
        assertEquals("Wrong aliases for 'test:kinderManager'", ALIASES_FOR_KINDER_MANAGER, new HashSet<>(relationRegistry.getAliases(TEST_KINDER_MANAGER)));
        assertEquals("Wrong aliases for 'manager'", ALIASES_FOR_KINDER_MANAGER, new HashSet<>(relationRegistry.getAliases(unqualify(TEST_KINDER_MANAGER))));

        Set<QName> ALIASES_FOR_TEST_APPROVER = new HashSet<>(singleton(TEST_APPROVER));
        assertEquals("Wrong aliases for 'test:approver'", ALIASES_FOR_TEST_APPROVER, new HashSet<>(relationRegistry.getAliases(TEST_APPROVER)));

        Set<QName> ALIASES_FOR_APPROVER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_APPROVER, unqualify(SchemaConstants.ORG_APPROVER)));
        assertEquals("Wrong aliases for 'approver'", ALIASES_FOR_APPROVER, new HashSet<>(relationRegistry.getAliases(unqualify(TEST_APPROVER))));

        Set<QName> RELATIONS_FOR_MEMBER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_DEFAULT, SchemaConstants.ORG_MANAGER));
        assertEquals("Wrong relations for MEMBER kind", RELATIONS_FOR_MEMBER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MEMBER)));

        Set<QName> RELATIONS_FOR_MANAGER = new HashSet<>(Arrays.asList(SchemaConstants.ORG_MANAGER, TEST_KINDER_MANAGER));
        assertEquals("Wrong relations for MANAGER kind", RELATIONS_FOR_MANAGER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MANAGER)));

        assertTrue("'org:default' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not processed on login", relationRegistry.isProcessedOnLogin(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not processed on login", relationRegistry.isProcessedOnLogin(null));
        assertTrue("'org:manager' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not processed on recompute", relationRegistry.isProcessedOnRecompute(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not processed on recompute", relationRegistry.isProcessedOnRecompute(null));
        assertTrue("'org:manager' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(null));
        assertTrue("'org:manager' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_MANAGER));
        assertFalse("'org:meta' is stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_META));
        assertFalse("'org:approver' is stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_APPROVER));

        assertTrue("'org:default' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_DEFAULT));
        assertTrue("'default' is not automatically matched", relationRegistry.isAutomaticallyMatched(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not automatically matched", relationRegistry.isAutomaticallyMatched(null));
        assertTrue("'org:manager' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_MANAGER));
        assertFalse("'org:meta' is automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_META));  // overridden
        assertFalse("'meta' is automatically matched", relationRegistry.isAutomaticallyMatched(unqualify(SchemaConstants.ORG_META))); // overridden
        assertTrue("'test:kinderManager' is not automatically matched", relationRegistry.isAutomaticallyMatched(TEST_KINDER_MANAGER));
        assertTrue("'kinderManager' is not automatically matched", relationRegistry.isAutomaticallyMatched(unqualify(TEST_KINDER_MANAGER)));
        assertFalse("'org:approver' is automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_APPROVER));

        String metaLabel = Objects.requireNonNull(relationRegistry.getRelationDefinition(SchemaConstants.ORG_META)).getDisplay().getLabel().getOrig();
        assertEquals("Wrong label for org:meta", "Meta-relation", metaLabel);
    }

    @Test
    public void test120ReplacedAllRelations() throws SchemaException, IOException {
        final String TEST_NAME = "test120ReplacedAllRelations";

        PrismObject<SystemConfigurationType> sysconfigObject = prismContext.parseObject(SYSCONFIG_REPLACED_ALL_RELATIONS_FILE);
        relationRegistry.applyRelationsConfiguration(sysconfigObject.asObjectable());

        assertEquals("Wrong # of relations", 4, relationRegistry.getRelationDefinitions().size());

        RelationDefinitionType orgDefaultDef = relationRegistry.getRelationDefinition(SchemaConstants.ORG_DEFAULT);
        RelationDefinitionType defaultDef = relationRegistry.getRelationDefinition(unqualify(SchemaConstants.ORG_DEFAULT));
        assertNull("Definition for 'org:default' relation exists", orgDefaultDef);
        assertNull("Definition for 'default' relation exists", defaultDef);

        RelationDefinitionType testKinderManagerDef = relationRegistry.getRelationDefinition(TEST_KINDER_MANAGER);
        RelationDefinitionType kinderManagerDef = relationRegistry.getRelationDefinition(unqualify(TEST_KINDER_MANAGER));
        RelationDefinitionType testExtraDef = relationRegistry.getRelationDefinition(TEST_EXTRA);
        RelationDefinitionType extraDef = relationRegistry.getRelationDefinition(unqualify(TEST_EXTRA));
        RelationDefinitionType nullDef = relationRegistry.getRelationDefinition(null);
        RelationDefinitionType testApproverDef = relationRegistry.getRelationDefinition(TEST_APPROVER);
        RelationDefinitionType approverDef = relationRegistry.getRelationDefinition(unqualify(TEST_APPROVER));
        assertNotNull("No definition for 'test:kinderManager' relation", testKinderManagerDef);
        assertEquals("'test:kinderManager' and 'kinderManager' definitions differ", testKinderManagerDef, kinderManagerDef);
        assertNotNull("No definition for 'test:extra' relation", testExtraDef);
        assertEquals("'test:extra' and 'extra' definitions differ", testExtraDef, extraDef);
        assertEquals("'test:extra' and null definitions differ", testExtraDef, nullDef);
        assertNotNull("No definition for 'test:approverDef' relation", testApproverDef);
        assertEquals("'test:approver' and 'approver' definitions differ", testApproverDef, approverDef);

        assertTrue("'test:kinderManager' is not of MANAGER kind", relationRegistry.isManager(TEST_KINDER_MANAGER));
        assertTrue("'kinderManager' is not of MANAGER kind", relationRegistry.isManager(unqualify(TEST_KINDER_MANAGER)));
        assertFalse("'null' is of MANAGER kind", relationRegistry.isManager(null));
        assertFalse("'test:extra' is of MANAGER kind", relationRegistry.isManager(TEST_EXTRA));
        assertFalse("'test:approver' is of MANAGER kind", relationRegistry.isManager(TEST_APPROVER));
        assertFalse("'approver' is of MANAGER kind", relationRegistry.isManager(unqualify(TEST_APPROVER)));

        assertFalse("'test:kinderManager' is of MEMBER kind", relationRegistry.isMember(TEST_KINDER_MANAGER));
        assertFalse("'kinderManager' is of MEMBER kind", relationRegistry.isMember(unqualify(TEST_KINDER_MANAGER)));
        assertTrue("'test:extra' is not of MEMBER kind", relationRegistry.isMember(TEST_EXTRA));
        assertTrue("'null' is not of MEMBER kind", relationRegistry.isMember(null));
        assertFalse("'test:approver' is of MEMBER kind", relationRegistry.isMember(TEST_APPROVER));
        assertFalse("'approver' is of MEMBER kind", relationRegistry.isMember(unqualify(TEST_APPROVER)));

        assertTrue("'test:approver' is not of APPROVER kind", relationRegistry.isApprover(TEST_APPROVER));
        assertTrue("'approver' is not of APPROVER kind", relationRegistry.isApprover(unqualify(TEST_APPROVER)));

        assertEquals("Wrong default relation", TEST_EXTRA, relationRegistry.getDefaultRelation());
        assertEquals("Wrong default MEMBER relation", TEST_EXTRA, relationRegistry.getDefaultRelationFor(RelationKindType.MEMBER));
        assertEquals("Wrong default MANAGER relation", TEST_KINDER_MANAGER, relationRegistry.getDefaultRelationFor(RelationKindType.MANAGER));
        assertEquals("Wrong default META relation", SchemaConstants.ORG_META, relationRegistry.getDefaultRelationFor(RelationKindType.META));

        Set<QName> ALIASES_FOR_DEFAULT = new HashSet<>(Arrays.asList(TEST_EXTRA, unqualify(TEST_EXTRA), null));
        assertEquals("Wrong aliases for 'test:extra'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(TEST_EXTRA)));
        assertEquals("Wrong aliases for 'extra'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(unqualify(TEST_EXTRA))));
        assertEquals("Wrong aliases for 'null'", ALIASES_FOR_DEFAULT, new HashSet<>(relationRegistry.getAliases(null)));

        Set<QName> ALIASES_FOR_ORG_MANAGER = new HashSet<>(singleton(SchemaConstants.ORG_MANAGER));
        assertEquals("Wrong aliases for 'org:manager'", ALIASES_FOR_ORG_MANAGER, new HashSet<>(relationRegistry.getAliases(SchemaConstants.ORG_MANAGER)));

        Set<QName> ALIASES_FOR_KINDER_MANAGER = new HashSet<>(Arrays.asList(TEST_KINDER_MANAGER, unqualify(TEST_KINDER_MANAGER)));
        assertEquals("Wrong aliases for 'test:kinderManager'", ALIASES_FOR_KINDER_MANAGER, new HashSet<>(relationRegistry.getAliases(TEST_KINDER_MANAGER)));
        assertEquals("Wrong aliases for 'manager'", ALIASES_FOR_KINDER_MANAGER, new HashSet<>(relationRegistry.getAliases(unqualify(TEST_KINDER_MANAGER))));

        Set<QName> ALIASES_FOR_TEST_APPROVER = new HashSet<>(Arrays.asList(TEST_APPROVER, unqualify(TEST_APPROVER)));
        assertEquals("Wrong aliases for 'test:approver'", ALIASES_FOR_TEST_APPROVER, new HashSet<>(relationRegistry.getAliases(TEST_APPROVER)));
        assertEquals("Wrong aliases for 'approver'", ALIASES_FOR_TEST_APPROVER, new HashSet<>(relationRegistry.getAliases(unqualify(TEST_APPROVER))));

        Set<QName> RELATIONS_FOR_MEMBER = new HashSet<>(singleton(TEST_EXTRA));
        assertEquals("Wrong relations for MEMBER kind", RELATIONS_FOR_MEMBER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MEMBER)));

        Set<QName> RELATIONS_FOR_MANAGER = new HashSet<>(singleton(TEST_KINDER_MANAGER));
        assertEquals("Wrong relations for MANAGER kind", RELATIONS_FOR_MANAGER, new HashSet<>(relationRegistry.getAllRelationsFor(RelationKindType.MANAGER)));

        assertFalse("'org:default' is processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_DEFAULT));
        assertFalse("'default' is processed on login", relationRegistry.isProcessedOnLogin(unqualify(SchemaConstants.ORG_DEFAULT)));
        assertTrue("'null' is not processed on login", relationRegistry.isProcessedOnLogin(null));
        assertFalse("'org:manager' is processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_MANAGER));
        assertTrue("'org:meta' is not processed on login", relationRegistry.isProcessedOnLogin(SchemaConstants.ORG_META));
        assertFalse("'test:approver' is processed on login", relationRegistry.isProcessedOnLogin(TEST_APPROVER));

        assertTrue("'null' is not processed on recompute", relationRegistry.isProcessedOnRecompute(null));
        assertTrue("'org:meta' is not processed on recompute", relationRegistry.isProcessedOnRecompute(SchemaConstants.ORG_META));
        assertFalse("'test:approver' is processed on recompute", relationRegistry.isProcessedOnRecompute(TEST_APPROVER));

        assertFalse("'org:default' is stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(SchemaConstants.ORG_DEFAULT));
        assertTrue("'null' is not stored into parentOrgRef", relationRegistry.isStoredIntoParentOrgRef(null));

        assertTrue("'null' is not automatically matched", relationRegistry.isAutomaticallyMatched(null));
        assertTrue("'org:meta' is not automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_META));  // overridden
        assertTrue("'test:kinderManager' is not automatically matched", relationRegistry.isAutomaticallyMatched(TEST_KINDER_MANAGER));
        assertTrue("'kinderManager' is not automatically matched", relationRegistry.isAutomaticallyMatched(unqualify(TEST_KINDER_MANAGER)));
        assertFalse("'org:approver' is automatically matched", relationRegistry.isAutomaticallyMatched(SchemaConstants.ORG_APPROVER));
    }

}
