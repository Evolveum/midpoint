/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_FOCUS_IDENTITY;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_FOCUS_NORMALIZED_DATA;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MFocusIdentity;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusIdentity;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusIdentityMapping;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.AlphabeticMethodExecutionRequired;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@AlphabeticMethodExecutionRequired
public class SqaleRepoIdentityDataTest extends SqaleRepoBaseTest {

    private static final File TEST_DIR = new File("src/test/resources/identity");
    private static final File FILE_USER_WITH_IDENTITY_DATA = new File(TEST_DIR, "user-with-identity-data.xml");

    private static Collection<SelectorOptions<GetOperationOptions>> getWithIdentitiesOptions;
    private static Collection<SelectorOptions<GetOperationOptions>> getWithIdentitiesNormalizedDataOptions;

    private String userOid;

    @BeforeClass
    public void init() {
        // This covers both identities/identity and identities/normalizedData skipped items.
        getWithIdentitiesOptions = SchemaService.get().getOperationOptionsBuilder()
                .item(FocusType.F_IDENTITIES).retrieve()
                .build();
        // This one only enforces identities/normalizedData fetching.
        getWithIdentitiesNormalizedDataOptions = SchemaService.get().getOperationOptionsBuilder()
                .item(PATH_FOCUS_NORMALIZED_DATA).retrieve()
                .build();
    }

    @Test
    public void test100AddUserWithIdentityData() throws CommonException, IOException {
        OperationResult result = createOperationResult();

        given("user with identity data");
        PrismObject<UserType> userToAdd = prismContext.parseObject(FILE_USER_WITH_IDENTITY_DATA);
        displayDumpable("user to add", userToAdd);
        displayValue("user to add (XML)", prismContext.xmlSerializer().serialize(userToAdd));

        when("addObject is called");
        userOid = repositoryService.addObject(userToAdd, null, result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test110GetUserByDefaultDoesNotLoadIdentityAndNormalizedData() throws CommonException {
        when("user is obtained from repo without retrieve options");
        OperationResult getResult = createOperationResult();
        UserType user = repositoryService.getObject(UserType.class, userOid, null, getResult).asObjectable();
        assertThatOperationResult(getResult).isSuccess();

        then("user's identity container and normalized data are empty and incomplete");
        PrismContainer<Containerable> identityContainer = user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY);
        assertThat(identityContainer.isIncomplete()).isTrue();
        assertThat(identityContainer.isEmpty()).isTrue();
        PrismContainer<Containerable> normalizedDataContainer = user.asPrismObject().findContainer(PATH_FOCUS_NORMALIZED_DATA);
        assertThat(normalizedDataContainer.isIncomplete()).isTrue();
        assertThat(normalizedDataContainer.isEmpty()).isTrue();

        and("default authoritative source is available, because it is not skipped");
        assertThat(user.getIdentities().getDefaultAuthoritativeSource()).isNotNull();
    }

    @Test
    public void test120GetUserWithRetrieveIdentitiesOptions() throws CommonException {
        when("user is obtained with retrieve options for identities");
        OperationResult getWithIdentitiesResult = createOperationResult();
        UserType user = repositoryService.getObject(UserType.class, userOid,
                getWithIdentitiesOptions, getWithIdentitiesResult).asObjectable();
        assertThatOperationResult(getWithIdentitiesResult).isSuccess();

        then("identities are complete and contain all the details");
        PrismContainer<Containerable> identityContainer = user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY);
        assertThat(identityContainer.isIncomplete()).isFalse();
        PrismContainer<Containerable> normalizedDataContainer = user.asPrismObject().findContainer(PATH_FOCUS_NORMALIZED_DATA);
        assertThat(normalizedDataContainer.isIncomplete()).isFalse();
        assertThat(user.getIdentities().getDefaultAuthoritativeSource()).isNotNull();

        List<FocusIdentityType> identities = user.getIdentities().getIdentity();
        assertThat(identities).hasSize(2);

        FocusNormalizedDataType normalizedData = user.getIdentities().getNormalizedData();
        assertThat(normalizedData).isNotNull();

        assertThat(normalizedDataContainer.getValue().getItems())
                .extracting(i -> i.getElementName().toString())
                .containsExactlyInAnyOrder("givenName", "familyName", "familyName.3", "dateOfBirth", "nationalId");

        // one of the identities will be checked thoroughly
        FocusIdentityType identity = identities.stream().filter(i -> i.getId().equals(1L)).findFirst().orElseThrow();
        FocusIdentitySourceType source = identity.getSource();
        ObjectReferenceType resourceRef = source.getResourceRef();
        assertThat(resourceRef).isNotNull()
                .extracting(r -> r.getOid())
                .isEqualTo("9dff5686-e695-4ad9-8098-5907758668c7");
    }

    @Test
    public void test130GetUserWithRetrieveIdentitiesNormalizedDataOptions() throws CommonException {
        when("user is obtained with retrieve options for identities/normalizedData");
        OperationResult getWithIdentitiesResult = createOperationResult();
        UserType user = repositoryService.getObject(UserType.class, userOid,
                getWithIdentitiesNormalizedDataOptions, getWithIdentitiesResult).asObjectable();
        assertThatOperationResult(getWithIdentitiesResult).isSuccess();

        then("identities have complete normalized data, but identity container is incomplete");
        PrismContainer<Containerable> normalizedDataContainer = user.asPrismObject().findContainer(PATH_FOCUS_NORMALIZED_DATA);
        assertThat(normalizedDataContainer.isIncomplete()).isFalse();
        assertThat(user.getIdentities().getNormalizedData()).isNotNull();

        // Technically, this can be also fetched (it's not forbidden), but it works as one would expect.
        PrismContainer<Containerable> identityContainer = user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY);
        assertThat(identityContainer.isIncomplete()).isTrue();
        assertThat(user.getIdentities().getIdentity()).isEmpty();
    }

    @Test
    public void test200SearchUsingNonFuzzyMatching() throws CommonException {
        given("query by focus identity normalized item");
        ItemName familyNameQName = new ItemName(SchemaConstants.NS_C, "familyName");
        var def = PrismContext.get().definitionFactory()
                .createPropertyDefinition(familyNameQName, DOMUtil.XSD_STRING, null, null);
        def.toMutable().setMaxOccurs(-1);

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .itemWithDef(def,
                        UserType.F_IDENTITIES,
                        FocusIdentitiesType.F_NORMALIZED_DATA,
                        familyNameQName)
                .eq("green")
                .build();

        when("search is executed without any get options");
        OperationResult result = createOperationResult();
        SearchResultList<UserType> users = searchObjects(UserType.class, query, result);
        assertThatOperationResult(result).isSuccess();

        then("result contains found user but identities are incomplete");
        assertThat(users).singleElement()
                .matches(u -> u.asPrismObject().findContainer(PATH_FOCUS_IDENTITY).isIncomplete());

        when("search is executed with retrieve items options");
        result = createOperationResult();
        users = searchObjects(UserType.class, query, result, getWithIdentitiesOptions);
        assertThatOperationResult(result).isSuccess();

        then("result contains round user with complete identities container");
        assertThat(users).hasSize(1);
        UserType user = users.get(0);
        assertThat(user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY).isIncomplete())
                .isFalse(); // is complete this time

        List<FocusIdentityType> identities = user.getIdentities().getIdentity();
        assertThat(identities).hasSize(2);
    }

    @Test
    public void test210SearchUsingFuzzyMatching() throws CommonException {
        given("query by focus identity normalized item using levenshtein");
        ItemName familyNameQName = new ItemName(SchemaConstants.NS_C, "familyName");
        var def = PrismContext.get().definitionFactory()
                .createPropertyDefinition(familyNameQName, DOMUtil.XSD_STRING, null, null);
        def.toMutable().setMaxOccurs(-1);

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .itemWithDef(def,
                        UserType.F_IDENTITIES,
                        FocusIdentitiesType.F_NORMALIZED_DATA,
                        familyNameQName)
                .fuzzyString("gren").levenshteinInclusive(3)
                .build();

        when("search is executed");
        OperationResult result = createOperationResult();
        SearchResultList<UserType> users = searchObjects(UserType.class, query, result);
        assertThatOperationResult(result).isSuccess();

        then("result contains found user");
        assertThat(users).hasSize(1);
        UserType user = users.get(0);
        assertThat(user.getOid()).isEqualTo(userOid);
    }

    @Test
    public void test300ModifyAddIdentityContainer() throws CommonException {
        OperationResult result = createOperationResult();

        given("delta adding focus identity value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(PATH_FOCUS_IDENTITY)
                .add(new FocusIdentityType()
                        .source(new FocusIdentitySourceType()
                                .tag("test300")))
                .asObjectDelta(userOid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("new focus identity row is added");
        QFocusIdentity<?> fi = QFocusIdentityMapping.get().defaultAlias();
        List<MFocusIdentity> fiRows = select(fi, fi.ownerOid.eq(UUID.fromString(userOid)));
        assertThat(fiRows).hasSize(3);

        and("getObject without identity options still has no identities");
        UserType user = repositoryService.getObject(UserType.class, userOid, null, result)
                .asObjectable();
        assertThat(user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY).isIncomplete())
                .isTrue();

        and("getObject returns focus with three identities");
        user = repositoryService.getObject(UserType.class, userOid, getWithIdentitiesOptions, result)
                .asObjectable();
        assertThat(user.getIdentities().getIdentity())
                .filteredOn(i -> i.getSource() != null && "test300".equals(i.getSource().getTag()))
                .singleElement()
                .extracting(i -> i.getData())
                .isNull();
    }

    @Test
    public void test310ModifyReplaceItemInIdentityContainer() throws CommonException {
        OperationResult result = createOperationResult();

        given("delta adding focus identity value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // 3 is container ID for identity PCV added in test300
                .item(PATH_FOCUS_IDENTITY.append(3L, FocusIdentityType.F_SOURCE, FocusIdentitySourceType.F_TAG))
                .replace("test310")
                .asObjectDelta(userOid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("getObject without identity options still has no identities/identity");
        UserType user = repositoryService.getObject(UserType.class, userOid, null, result)
                .asObjectable();
        assertThat(user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY).isIncomplete()).isTrue();

        and("getObject returns focus with three identities");
        user = repositoryService.getObject(UserType.class, userOid, getWithIdentitiesOptions, result)
                .asObjectable();
        assertThat(user.getIdentities().getIdentity())
                .filteredOn(i -> i.getSource() != null && "test310".equals(i.getSource().getTag()))
                .singleElement()
                .matches(i -> i.getId().equals(3L));
    }

    @Test
    public void test320ModifyReplaceItemInIdentityContainerJsonbColumn() throws CommonException {
        OperationResult result = createOperationResult();

        given("delta adding focus identity normalized item");
        ItemName familyNameQName = new ItemName(SchemaConstants.NS_C, "familyName");
        var def = PrismContext.get().definitionFactory()
                .createPropertyDefinition(familyNameQName, DOMUtil.XSD_STRING, null, null);

        ItemPath itemPath = PATH_FOCUS_NORMALIZED_DATA.append(familyNameQName);
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(itemPath, def)
                .replace("blue") // no more green, sorry
                .asObjectDelta(userOid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("getObject without identity options still has no identities");
        UserType user = repositoryService.getObject(UserType.class, userOid, null, result)
                .asObjectable();
        assertThat(user.asPrismObject().findContainer(PATH_FOCUS_IDENTITY).isIncomplete()).isTrue();

        and("getObject returns focus with three identities");
        user = repositoryService.getObject(UserType.class, userOid, getWithIdentitiesOptions, result)
                .asObjectable();
        assertThat(user.getIdentities().getNormalizedData().prismGetPropertyValue(familyNameQName, String.class))
                .as("normalized family name")
                .isEqualTo("blue");
    }
}
