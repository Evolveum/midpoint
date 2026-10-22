/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Covers the objects that the work item row transformer pre-attaches to certification case references
 * when the {@code resolveNames} option is used. Whatever object is attached must be usable
 * by the GUI: {@code asObjectable()} must not fail and display names must be correct, for every
 * object type that can appear as a certification case object or target (including resources and policies).
 *
 * Issue: 12829
 */
public class SqaleRepoCertWorkItemPreloadedObjectTest extends SqaleRepoBaseTest {

    private static final String USER_FULL_NAME = "Cert User Full";
    private static final String USER_DISPLAY_NAME = "Cert User Display";
    private static final String ROLE_DISPLAY_NAME = "Cert Role Display";
    private static final String ORG_DISPLAY_NAME = "Cert Org Display";
    private static final String POLICY_DISPLAY_NAME = "Cert Policy Display";
    private static final String RESOURCE_NAME = "cert-preload-resource";

    private String userOid;
    private String userWithDisplayNameOid;
    private String roleOid;
    private String orgOid;
    private String policyOid;
    private String resourceOid;
    private String campaignOid;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        userOid = repositoryService.addObject(
                new UserType().name("cert-preload-user").fullName(USER_FULL_NAME).asPrismObject(), null, result);
        userWithDisplayNameOid = repositoryService.addObject(
                new UserType().name("cert-preload-user-2").fullName(USER_FULL_NAME).displayName(USER_DISPLAY_NAME)
                        .asPrismObject(), null, result);
        roleOid = repositoryService.addObject(
                new RoleType().name("cert-preload-role").displayName(ROLE_DISPLAY_NAME).asPrismObject(), null, result);
        orgOid = repositoryService.addObject(
                new OrgType().name("cert-preload-org").displayName(ORG_DISPLAY_NAME).asPrismObject(), null, result);
        policyOid = repositoryService.addObject(
                new PolicyType().name("cert-preload-policy").displayName(POLICY_DISPLAY_NAME).asPrismObject(), null, result);
        resourceOid = repositoryService.addObject(
                new ResourceType().name(RESOURCE_NAME).asPrismObject(), null, result);

        campaignOid = repositoryService.addObject(
                new AccessCertificationCampaignType().name("cert-preload-campaign")
                        .stageNumber(1)
                        .iteration(1)
                        ._case(certCase(1L, userOid, roleOid, RoleType.COMPLEX_TYPE))
                        ._case(certCase(2L, userOid, orgOid, OrgType.COMPLEX_TYPE))
                        ._case(certCase(3L, userOid, policyOid, PolicyType.COMPLEX_TYPE))
                        ._case(certCase(4L, userOid, resourceOid, ResourceType.COMPLEX_TYPE))
                        ._case(certCase(5L, userWithDisplayNameOid, roleOid, RoleType.COMPLEX_TYPE))
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    private AccessCertificationCaseType certCase(long id, String objectOid, String targetOid, QName targetType) {
        return new AccessCertificationCaseType()
                .id(id)
                .stageNumber(1)
                .iteration(1)
                .objectRef(objectOid, UserType.COMPLEX_TYPE)
                .targetRef(targetOid, targetType)
                .workItem(new AccessCertificationWorkItemType()
                        .stageNumber(1)
                        .iteration(1)
                        .assigneeRef(userOid, UserType.COMPLEX_TYPE));
    }

    /** This is the scenario behind issue 12829: the GUI calls asObjectable() on the pre-attached object. */
    @Test
    public void test100PreloadedObjectsAreMaterializable() throws Exception {
        forEachCase(aCase -> {
            assertMaterializable(aCase.getObjectRef());
            assertMaterializable(aCase.getTargetRef());
        });
    }

    @Test
    public void test110PreloadedObjectsHaveExpectedDisplayNames() throws Exception {
        forEachCase(aCase -> {
            ObjectReferenceType targetRef = aCase.getTargetRef();
            String expectedTargetDisplayName;
            if (QNameUtil.match(targetRef.getType(), RoleType.COMPLEX_TYPE)) {
                expectedTargetDisplayName = ROLE_DISPLAY_NAME;
            } else if (QNameUtil.match(targetRef.getType(), OrgType.COMPLEX_TYPE)) {
                expectedTargetDisplayName = ORG_DISPLAY_NAME;
            } else if (QNameUtil.match(targetRef.getType(), PolicyType.COMPLEX_TYPE)) {
                expectedTargetDisplayName = POLICY_DISPLAY_NAME;
            } else if (QNameUtil.match(targetRef.getType(), ResourceType.COMPLEX_TYPE)) {
                expectedTargetDisplayName = RESOURCE_NAME;
            } else {
                throw new AssertionError("Unexpected target type " + targetRef.getType());
            }
            assertDisplayName(targetRef, expectedTargetDisplayName);

            String expectedObjectDisplayName = userWithDisplayNameOid.equals(aCase.getObjectRef().getOid())
                    ? USER_DISPLAY_NAME : USER_FULL_NAME;
            assertDisplayName(aCase.getObjectRef(), expectedObjectDisplayName);
        });
    }

    @Test
    public void test120PreloadedObjectsHaveTypeMatchingTheReference() throws Exception {
        forEachCase(aCase -> {
            assertTypeMatchesReference(aCase.getObjectRef());
            assertTypeMatchesReference(aCase.getTargetRef());
        });
    }

    private void forEachCase(Consumer<AccessCertificationCaseType> check) throws Exception {
        OperationResult result = createOperationResult();
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());

        SearchResultList<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class,
                prismContext.queryFor(AccessCertificationWorkItemType.class)
                        .ownedBy(AccessCertificationCaseType.class)
                        .ownedBy(AccessCertificationCampaignType.class)
                        .id(campaignOid)
                        .build(),
                options, result);

        assertThatOperationResult(result).isSuccess();
        assertThat(workItems).hasSize(5);
        for (AccessCertificationWorkItemType workItem : workItems) {
            AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
            assertThat(aCase.getObjectRef()).isNotNull();
            assertThat(aCase.getTargetRef()).isNotNull();
            check.accept(aCase);
        }
    }

    /** A pre-attached object is optional (the GUI loads the object when it is missing), but if present it must work. */
    private void assertMaterializable(ObjectReferenceType ref) {
        PrismObject<?> object = ref.asReferenceValue().getObject();
        if (object == null) {
            return;
        }
        ObjectType objectable;
        try {
            objectable = (ObjectType) object.asObjectable();
        } catch (RuntimeException e) {
            throw new AssertionError("Pre-attached object for reference of type " + ref.getType().getLocalPart()
                    + " cannot be materialized: " + e.getMessage(), e);
        }
        assertThat(objectable.getOid()).as("oid of pre-attached object for " + ref.getType().getLocalPart())
                .isEqualTo(ref.getOid());
        assertThat(objectable.getName()).as("name of pre-attached object for " + ref.getType().getLocalPart())
                .isNotNull();
    }

    private void assertDisplayName(ObjectReferenceType ref, String expected) {
        PrismObject<?> object = ref.asReferenceValue().getObject();
        if (object == null) {
            return;
        }
        assertThat(PolyString.getOrig(ObjectTypeUtil.getDisplayName(object)))
                .as("display name of pre-attached object for " + ref.getType().getLocalPart())
                .isEqualTo(expected);
    }

    private void assertTypeMatchesReference(ObjectReferenceType ref) {
        PrismObject<?> object = ref.asReferenceValue().getObject();
        if (object == null) {
            return;
        }
        Class<?> expectedClass = prismContext.getSchemaRegistry().determineClassForType(ref.getType());
        assertThat(object.getCompileTimeClass())
                .as("class of pre-attached object for reference of type " + ref.getType().getLocalPart())
                .isEqualTo(expectedClass);
    }
}
