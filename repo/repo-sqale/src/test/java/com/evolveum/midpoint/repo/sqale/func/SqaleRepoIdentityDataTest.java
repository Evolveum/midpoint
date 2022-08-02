/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MFocusIdentity;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusIdentity;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusIdentityMapping;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.AlphabeticMethodExecutionRequired;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@AlphabeticMethodExecutionRequired
public class SqaleRepoIdentityDataTest extends SqaleRepoBaseTest {

    public static final File TEST_DIR = new File("src/test/resources/identity");

    private static final File FILE_USER_WITH_IDENTITY_DATA = new File(TEST_DIR, "user-with-identity-data.xml");

    private static Collection<SelectorOptions<GetOperationOptions>> getWithIdentitiesOptions;

    private String userOid;

    @BeforeClass
    public void initObjects() {
        OperationResult result = createOperationResult();

        // TODO

        assertThatOperationResult(result).isSuccess();

        getWithIdentitiesOptions = SchemaService.get().getOperationOptionsBuilder()
                .item(FocusType.F_IDENTITIES).retrieve()
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
    public void test110GetUserByDefaultDoesNotLoadIdentityData() throws CommonException {
        when("user is obtained from repo without retrieve options");
        OperationResult getResult = createOperationResult();
        UserType user = repositoryService.getObject(UserType.class, userOid, null, getResult).asObjectable();
        assertThatOperationResult(getResult).isSuccess();

        then("user's identity container is empty and incomplete");
        assertThat(((PrismContainerValue<?>) user.getIdentities().asPrismContainerValue()).isEmpty()).isTrue();
        assertThat(user.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isTrue();
    }

    @Test
    public void test115GetUserWithRetrieveOptions() throws CommonException {
        when("user is obtained with retrieve options for identities");
        OperationResult getWithIdentitiesResult = createOperationResult();
        UserType user2 = repositoryService.getObject(UserType.class, userOid,
                getWithIdentitiesOptions, getWithIdentitiesResult).asObjectable();
        assertThatOperationResult(getWithIdentitiesResult).isSuccess();

        then("identities are complete and contain all the details");
        PrismContainer<Containerable> identitiesContainer = user2.asPrismObject().findContainer(FocusType.F_IDENTITIES);
        assertThat(identitiesContainer.isIncomplete()).isFalse();

        List<FocusIdentityType> identities = user2.getIdentities().getIdentity();
        assertThat(identities).hasSize(2);

        // one of the identities will be checked thoroughly
        FocusIdentityType identity = identities.stream().filter(i -> i.getId().equals(1L)).findFirst().orElseThrow();
        // Internally we skip this for m_focus_identity.fullObject, but this is only implementation detail
        // and we don't want to propagate it up. Items are retrieved without any need for explicit retrieve option.
        assertThat(identity.asPrismContainerValue().findContainer(FocusIdentityType.F_ITEMS).isIncomplete()).isFalse();
        FocusIdentitySourceType source = identity.getSource();
        ObjectReferenceType resourceRef = source.getResourceRef();
        assertThat(resourceRef).isNotNull()
                .extracting(r -> r.getOid())
                .isEqualTo("9dff5686-e695-4ad9-8098-5907758668c7");

        assertThat(Containerable.asPrismContainerValue(identity.getItems().getOriginal()).getItems())
                .extracting(i -> i.getElementName().toString())
                .containsExactlyInAnyOrder("givenName", "familyName", "dateOfBirth", "nationalId");
        assertThat(Containerable.asPrismContainerValue(identity.getItems().getNormalized()).getItems())
                .extracting(i -> i.getElementName().toString())
                .containsExactlyInAnyOrder("givenName", "familyName", "familyName.3", "dateOfBirth", "nationalId");
    }

    @Test
    public void test200SearchUsingFuzzyMatching() throws CommonException {
        OperationResult result = createOperationResult();
        ItemName familyNameQName = new ItemName(SchemaConstants.NS_C, "familyName");
        var def = PrismContext.get().definitionFactory().createPropertyDefinition(familyNameQName, DOMUtil.XSD_STRING, null, null);
        def.toMutable().setRuntimeSchema(true);

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .itemWithDef(def,
                        UserType.F_IDENTITIES,
                        FocusIdentitiesType.F_IDENTITY,
                        FocusIdentityType.F_ITEMS,
                        FocusIdentityItemsType.F_NORMALIZED,
                        familyNameQName)
                .eq("green") // TODO was alice, do you want givenName query?
                .build();
        var ret = repositoryService.searchObjects(UserType.class, query, null, result); // TODO use retrieve options and assert?
        assertThat(ret.size()).isEqualTo(1);
    }

    @Test
    public void test300ModifyAddIdentityContainer() throws CommonException {
        OperationResult result = createOperationResult();

        given("delta adding focus identity value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY)
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
        assertThat(user.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isTrue();

        and("getObject returns focus with three identities");
        user = repositoryService.getObject(UserType.class, userOid, getWithIdentitiesOptions, result)
                .asObjectable();
        assertThat(user.getIdentities().getIdentity())
                .filteredOn(i -> i.getSource() != null && "test300".equals(i.getSource().getTag()))
                .singleElement()
                .extracting(i -> i.getItems())
                .isNull();
    }

    @Test
    public void test310ModifyReplaceItemInIdentityContainer() throws CommonException {
        OperationResult result = createOperationResult();

        given("delta adding focus identity value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // 3 is container ID for identity PCV added in test300
                .item(UserType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY, 3L,
                        FocusIdentityType.F_SOURCE, FocusIdentitySourceType.F_TAG)
                .replace("test310")
                .asObjectDelta(userOid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("getObject without identity options still has no identities");
        UserType user = repositoryService.getObject(UserType.class, userOid, null, result)
                .asObjectable();
        assertThat(user.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isTrue();

        and("getObject returns focus with three identities");
        user = repositoryService.getObject(UserType.class, userOid, getWithIdentitiesOptions, result)
                .asObjectable();
        assertThat(user.getIdentities().getIdentity())
                .filteredOn(i -> i.getSource() != null && "test310".equals(i.getSource().getTag()))
                .singleElement()
                .matches(i -> i.getId().equals(3L));
    }

    // TODO modification test inside items, check JSONB
}
