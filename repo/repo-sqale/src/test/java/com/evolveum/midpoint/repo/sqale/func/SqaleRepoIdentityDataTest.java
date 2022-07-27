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

import javax.xml.namespace.QName;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

// TEMPORARY CODE
public class SqaleRepoIdentityDataTest extends SqaleRepoBaseTest {

    public static final File TEST_DIR = new File("src/test/resources/identity");

    private static final File FILE_USER_WITH_IDENTITY_DATA = new File(TEST_DIR, "user-with-identity-data.xml");

    private String userOid;

    @BeforeClass
    public void initObjects() {
        OperationResult result = createOperationResult();

        // TODO

        assertThatOperationResult(result).isSuccess();
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
        Collection<SelectorOptions<GetOperationOptions>> getOptions = SchemaService.get()
                .getOperationOptionsBuilder().item(FocusType.F_IDENTITIES).retrieve().build();
        OperationResult getWithIdentitiesResult = createOperationResult();
        UserType user2 = repositoryService.getObject(UserType.class, userOid, getOptions, getWithIdentitiesResult).asObjectable();
        assertThatOperationResult(getWithIdentitiesResult).isSuccess();

        then("identities are complete and contain all the details");
        assertThat(user2.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isFalse();

        List<FocusIdentityType> identities = user2.getIdentities().getIdentity();
        assertThat(identities).hasSize(2);

        // one of the identities will be checked thoroughly
        FocusIdentityType identity = identities.stream().filter(i -> i.getId().equals(1L)).findFirst().orElseThrow();
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
    public void testSeachUsingFuzzyMatching() throws CommonException {
        when("user is obtained with retrieve options for identities");
        Collection<SelectorOptions<GetOperationOptions>> getOptions = SchemaService.get()
                .getOperationOptionsBuilder().item(FocusType.F_IDENTITIES).retrieve().build();
        OperationResult result = createOperationResult();
        ItemName familyNameQName = new ItemName(SchemaConstants.NS_MIDPOINT_PUBLIC_COMMON, "familyName");
        var  def = PrismContext.get().definitionFactory().createPropertyDefinition(familyNameQName, DOMUtil.XSD_STRING, null, null);
        def.toMutable().setRuntimeSchema(true);

        ObjectQuery query = PrismContext.get().queryFor(UserType.class).itemWithDef(def, UserType.F_IDENTITIES,
                FocusIdentitiesType.F_IDENTITY,
                FocusIdentityType.F_ITEMS,
                FocusIdentityItemsType.F_NORMALIZED,
                new ItemName(SchemaConstants.NS_MIDPOINT_PUBLIC_COMMON, "familyName")
                ).eq("alice").build();
        var ret = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThat(ret.size()).isEqualTo(1);
    }
    // TODO modification test + hopefully updateGetOptions in QFocusMapping does the trick
}
