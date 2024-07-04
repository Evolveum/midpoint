/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.misc;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.prism.polystring.NormalizerRegistry;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests the repository support needed for the shadow caching.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestShadowCachingSupport extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "misc/caching");

    /** This is just to provide some schemas. */
    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy.xml", "540498ba-c163-4f38-91d8-a0dc96e6f936", null,
            c -> c.extendSchemaPirate());

    @Autowired private NormalizerRegistry normalizerRegistry;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY.initAndTest(this, initTask, initResult);
    }

    @Test
    public void test100AddModifyDeleteShadow() throws CommonException {

        var result = getTestOperationResult();

        given("a shadow");

        var accountDef = Resource.of(RESOURCE_DUMMY.get())
                .getCompleteSchemaRequired()
                .findObjectDefinitionRequired(ShadowKindType.ACCOUNT, INTENT_DEFAULT);

        String name = "test100.Add-Modify-Delete-Shadow";
        String weapon = "cn=  Sword,   dc=ExAmPLE,dc=cOm  ";
        String gossip = "don't tell anyone";
        ShadowSimpleAttribute<String> nameAttr = accountDef.instantiateAttribute(ICFS_NAME, name);
        ShadowSimpleAttribute<String> weaponAttr = accountDef.instantiateAttribute(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, weapon);
        ShadowSimpleAttribute<String> gossipAttr = accountDef.instantiateAttribute(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, gossip);
        var nameRepoAttrDef = nameAttr.getDefinitionRequired().toNormalizationAware();
        var weaponRepoAttrDef = weaponAttr.getDefinitionRequired().toNormalizationAware();
        var gossipRepoAttrDef = gossipAttr.getDefinitionRequired().toNormalizationAware();
        PrismProperty<?> nameRepoAttr = nameRepoAttrDef.adoptRealValuesAndInstantiate(List.of(name));
        PrismProperty<?> weaponRepoAttr = weaponRepoAttrDef.adoptRealValuesAndInstantiate(List.of(weapon));
        PrismProperty<?> gossipRepoAttr = gossipRepoAttrDef.adoptRealValuesAndInstantiate(List.of(gossip));
        String nameNorm = stringCaseIgnoreNormalizer().normalizeString(name);
        String weaponNorm = distinguishedNameNormalizer().normalizeString(weapon);

        String newName = "test100.Add-Modify-Delete-Shadow-Changed";
        String newWeapon = "cn=  pistol,   dc=ExAmPLE,dc=cOm  ";
        String newGossip = "NEVER";
        PrismProperty<?> newNameRepoAttr = nameRepoAttrDef.adoptRealValuesAndInstantiate(List.of(newName));
        PrismProperty<?> newWeaponRepoAttr = weaponRepoAttrDef.adoptRealValuesAndInstantiate(List.of(newWeapon));
        PrismProperty<?> newGossipRepoAttr = gossipRepoAttrDef.adoptRealValuesAndInstantiate(List.of(newGossip));
        String newNameNorm = stringCaseIgnoreNormalizer().normalizeString(newName);
        String newWeaponNorm = distinguishedNameNormalizer().normalizeString(newWeapon);

        ShadowType shadow = new ShadowType()
                .name(name)
                .resourceRef(RESOURCE_DUMMY.ref())
                .objectClass(RI_ACCOUNT_OBJECT_CLASS);

        ShadowAttributesContainer attrContainer = ShadowUtil.setupAttributesContainer(shadow, accountDef);
        attrContainer.addAttribute(nameAttr.clone());
        attrContainer.addAttribute(weaponAttr.clone());
        attrContainer.addAttribute(gossipAttr.clone());

        ShadowType repoShadow = new ShadowType()
                .name(name)
                .resourceRef(RESOURCE_DUMMY.ref())
                .objectClass(RI_ACCOUNT_OBJECT_CLASS);

        PrismContainer<ShadowAttributesType> repoAttrContainer =
                repoShadow.asPrismObject().findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        repoAttrContainer.add(nameRepoAttr.clone());
        repoAttrContainer.add(weaponRepoAttr.clone());
        repoAttrContainer.add(gossipRepoAttr.clone());

        display("repo shadow", repoShadow);

        when("adding the shadow to the repository");
        repositoryService.addObject(repoShadow.asPrismObject(), null, result);

        then("the shadow is in the repository");
        var repoShadowAfter = repositoryService.getObject(ShadowType.class, repoShadow.getOid(), null, result);

        and("the shadow has the expected attributes");
        ShadowAsserter.forRepoShadow(repoShadowAfter, getCachedAttributeNames())
                .display()
                .assertName(name)
                .assertCachedOrigValues(ICFS_NAME, name)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, weapon)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, gossip)
                .assertCachedNormValues(ICFS_NAME, nameNorm)
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, weaponNorm)
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, gossip);

        when("searching for the shadow in the repository");
        trySearch(nameRepoAttr, name, nameNorm, PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, name, "x", PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, "x", nameNorm, PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, name, nameNorm, PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, name, "x", PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, "x", nameNorm, PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 1, result);

        when("modifying the shadow in the repository");
        repositoryService.modifyObject(
                ShadowType.class,
                repoShadow.getOid(),
                deltaFor(ShadowType.class)
                        .item(ICFS_NAME_PATH, nameRepoAttrDef).replaceRealValues(newNameRepoAttr.getRealValues())
                        .item(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH, weaponRepoAttrDef).replaceRealValues(newWeaponRepoAttr.getRealValues())
                        .item(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH, gossipRepoAttrDef).replaceRealValues(newGossipRepoAttr.getRealValues())
                        .asItemDeltas(),
                result);

        then("the updated shadow is in the repository");
        var newRepoShadowAfter = repositoryService.getObject(ShadowType.class, repoShadow.getOid(), null, result);

        and("the updated shadow has the expected attributes");
        ShadowAsserter.forRepoShadow(newRepoShadowAfter, getCachedAttributeNames())
                .display()
                .assertName(name)
                .assertCachedOrigValues(ICFS_NAME, newName)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, newWeapon)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, newGossip)
                .assertCachedNormValues(ICFS_NAME, newNameNorm)
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, newWeaponNorm)
                .assertCachedNormValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, newGossip);

        when("searching for the updated shadow in the repository");
        trySearch(nameRepoAttr, name, nameNorm, PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, newName, newNameNorm, PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, newName, "x", PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, "x", newNameNorm, PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, name, nameNorm, PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, newName, newNameNorm, PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 1, result);
        trySearch(nameRepoAttr, newName, "x", PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 0, result);
        trySearch(nameRepoAttr, "x", newNameNorm, PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, 1, result);
    }

    private void trySearch(
            PrismProperty<?> repoAttr, String orig, String norm, QName matchingRule, int expectedFound, OperationResult result)
            throws SchemaException {
        var objects = repositoryService.searchObjects(
                ShadowType.class,
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_ATTRIBUTES.append(repoAttr.getElementName()), repoAttr.getDefinition())
                        .eqPoly(orig, norm)
                        .matching(matchingRule)
                        .build(),
                null, result);
        assertThat(objects)
                .as("shadows found for %s (%s, %s, %s)".formatted(repoAttr, orig, norm, matchingRule))
                .hasSize(expectedFound);
    }

    private @NotNull Normalizer<String> stringCaseIgnoreNormalizer() {
        return normalizerRegistry.getNormalizerRequired(PrismConstants.LOWERCASE_STRING_NORMALIZER);
    }

    private @NotNull Normalizer<String> distinguishedNameNormalizer() {
        return normalizerRegistry.getNormalizerRequired(PrismConstants.DISTINGUISHED_NAME_NORMALIZER);
    }

    private Collection<? extends QName> getCachedAttributeNames() {
        return List.of(ICFS_NAME, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME, DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME);
    }
}
