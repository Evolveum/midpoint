/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.prism.PrismConstants.T_OBJECT_REFERENCE;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;

import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Tests not just searchObjects, but also how this behaves after modification
 * and reindex after configuration change.
 *
 * WARNING: Index-only extension/attributes full-text indexing is not reliable.
 *
 * TODO: Is there any possibility to search for whole words, start/end of the word?
 *  Currently the index should allow it (using space before the first and after the last word
 *  for this reason), but it is unclear how to indicate start/end of the word in the search
 *  string (space separates word searches combined with AND).
 */
public class SqaleSearchFullTextTest extends SqaleRepoBaseTest {

    private String user1Oid;
    private String user2Oid;
    private String user3Oid;
    private String user4Oid;
    private String task1Oid;
    private String task2Oid;

    private String roleOid;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        repositoryService.applyFullTextSearchConfiguration(
                new FullTextSearchConfigurationType()
                        .indexed(new FullTextSearchIndexedItemsConfigurationType()
                                .item(new ItemPathType(ObjectType.F_NAME))
                                .item(new ItemPathType(ObjectType.F_DESCRIPTION))));

        roleOid = repositoryService.addObject(
                new RoleType().name("Test Role")
                        .description("role which should be found when search for swashbuckling")
                        .assignment(new AssignmentType()
                                .lifecycleState("for-ownedBy-test"))
                        .asPrismObject(),
                null, result);

        UserType user1 = new UserType().name("user-1")
                .fullName("User Name 1")
                .metadata(new MetadataType()
                        .createChannel("create-channel")
                        .createTimestamp(asXMLGregorianCalendar(1L))
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(asXMLGregorianCalendar(2L)))
                .subtype("workerA")
                .subtype("workerC")
                .policySituation("situationA")
                .policySituation("situationC")
                .assignment(new AssignmentType()
                        .description("assignment one description")
                        .lifecycleState("assignment1-1")
                        .subtype("ass-subtype-2")
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .description("assignment two description")
                        .lifecycleState("assignment1-2"))
                .extension(new ExtensionType());
        // extension can be full-text indexed too, but not for index-only stuff
        ExtensionType user1Extension = user1.getExtension();
        addExtensionValue(user1Extension, "string", "indexable string extension");
        addExtensionValue(user1Extension, "long", 747L);
        addExtensionValue(user1Extension, "string-mv", "multi-value string", "another multi-value string");
        user1Oid = repositoryService.addObject(user1.asPrismObject(), null, result);

        UserType user2 = new UserType().name("user-2")
                .description("user description with many repetitions of word user and user again")
                .subtype("workerA")
                .activation(new ActivationType()
                        .validFrom("2021-03-01T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z"))
                .metadata(new MetadataType()
                        .createTimestamp(asXMLGregorianCalendar(2L)));
        user2Oid = repositoryService.addObject(user2.asPrismObject(), null, result);

        UserType user3 = new UserType().name("user-3")
                .description("Slovenský opis ľahkovážnej osoby číslo TŘI")
                .costCenter("50")
                .policySituation("situationA")
                .metadata(new MetadataType()
                        .createApproverRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT))
                .assignment(new AssignmentType()
                        .lifecycleState("ls-user3-ass1")
                        .activation(new ActivationType()
                                .validFrom("2021-01-01T00:00:00Z"))
                        .subtype("ass-subtype-1")
                        .subtype("ass-subtype-2"))
                .assignment(new AssignmentType()
                        .activation(new ActivationType()
                                .validTo("2022-01-01T00:00:00Z")));
        user3Oid = repositoryService.addObject(user3.asPrismObject(), null, result);

        user4Oid = repositoryService.addObject(
                new UserType().name("user-4")
                        .description("I like tasks task1 and especiallytask2") // intentionally glued
                        .givenName("John")
                        .fullName("John")
                        .costCenter("51")
                        .subtype("workerB")
                        .policySituation("situationB")
                        .organization("org-1") // orgs and ous are polys stored in JSONB arrays
                        .organization("org-2")
                        .organizationalUnit("ou-1")
                        .organizationalUnit("ou-2")
                        .asPrismObject(),
                null, result);

        // other objects
        task1Oid = repositoryService.addObject(
                new TaskType().name("task-1")
                        .executionState(TaskExecutionStateType.RUNNABLE)
                        .asPrismObject(),
                null, result);
        task2Oid = repositoryService.addObject(
                new TaskType().name("task-2")
                        .executionState(TaskExecutionStateType.CLOSED)
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100SearchWithEmptyFullTextFilterReturnsEverything() throws Exception {
        searchObjectTest("with empty full-text query",
                ObjectType.class,
                f -> f.fullText(""),
                user1Oid, user2Oid, user3Oid, user4Oid, task1Oid, task2Oid, roleOid);
    }

    @Test
    public void test110SearchWithFullTextQueryMatchingNothing() throws Exception {
        searchUsersTest("with full-text matching nothing",
                f -> f.fullText("xxx-nonexistent"));
    }

    @Test
    public void test120SearchAnythingContainingOneSubstring() throws Exception {
        searchObjectTest("with full-text for 'task' anywhere",
                ObjectType.class,
                f -> f.fullText("task"),
                task1Oid, task2Oid, user4Oid);
    }

    @Test
    public void test122SearchAnythingContainingMultipleWords() throws Exception {
        searchObjectTest("with full-text containing two strings in any order",
                ObjectType.class,
                f -> f.fullText("task user"),
                user4Oid);
    }

    @Test
    public void test130SearchNormalizesInput() throws Exception {
        searchObjectTest("with full-text containing two strings with diacritics",
                ObjectType.class,
                f -> f.fullText("úšer Ťäsk"),
                user4Oid);
    }

    @Test
    public void test140SearchWorksAgainstNormalizedValues() throws Exception {
        searchObjectTest("with full-text searching for objects using diacritics",
                ObjectType.class,
                f -> f.fullText("vaznej"),
                user3Oid);
    }

    @Test
    public void test200FullTextWithMultipleValuesIsNotSupportedYet() {
        assertThatThrownBy(() ->
                searchObjectTest("with full-text using multiple values",
                        ObjectType.class,
                        f -> f.fullText("val1", "val2")))
                .isInstanceOf(SystemException.class)
                .hasMessage("FullText filter currently supports only a single string");
    }

    @Test
    public void test220SearchInReferenceTargetObject() throws Exception {
        searchUsersTest("having assignment to a role matching the fulltext condition",
                f -> f.exists(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE)
                        .fullText("swashbuckling"),
                user1Oid);
    }

    @Test
    public void test230SearchInReferencingObject() throws Exception {
        searchUsersTest("referenced by another object matching the fulltext condition",
                f -> f.referencedBy(UserType.class,
                                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF))
                        .fullText("TŘI číslo"), // will be normalized
                user1Oid);
    }

    @Test
    public void test240SearchInOwnerObject() throws Exception {
        SearchResultList<AssignmentType> result = searchContainerTest(
                "with owner using fulltext", AssignmentType.class,
                f -> f.ownedBy(AbstractRoleType.class)
                        .fullText("swashbuckling"));

        assertThat(result).singleElement()
                .extracting(a -> a.getLifecycleState())
                .isEqualTo("for-ownedBy-test");
    }

    @Test
    public void test300FullTextConfigurationWithoutReindexDoesNotAffectResults()
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        repositoryService.applyFullTextSearchConfiguration(
                new FullTextSearchConfigurationType()
                        .indexed(new FullTextSearchIndexedItemsConfigurationType()
                                .item(new ItemPathType(ObjectType.F_NAME))));

        searchObjectTest("with full-text search against description before reindex",
                ObjectType.class,
                f -> f.fullText("vaznej"),
                user3Oid);

        repositoryService.modifyObject(UserType.class, user3Oid, List.of(),
                RepoModifyOptions.createForceReindex(), createOperationResult());

        searchObjectTest("with full-text search against description after reindex",
                ObjectType.class,
                f -> f.fullText("vaznej"));
        // nobody matches now, good
    }

    @Test
    public void test400FullTextOfMultiValueContainerItemsIsNotSupported() {
        repositoryService.applyFullTextSearchConfiguration(
                new FullTextSearchConfigurationType()
                        .indexed(new FullTextSearchIndexedItemsConfigurationType()
                                .item(new ItemPathType(ItemPath.create(
                                        AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION)))));

        // TODO: this is potentially fixable, but is now a documented limitation.
        //  Also, when this is supported, it will make accCertCampaign/case full-text unreliable,
        //  just like index-only extension/attributes are (also documented limitation).
        assertThatThrownBy(() ->
                repositoryService.modifyObject(UserType.class, user1Oid, List.of(),
                        RepoModifyOptions.createForceReindex(), createOperationResult()))
                .isInstanceOf(SystemException.class)
                .hasMessage("Attempt to get segment without an ID from a multi-valued container assignment");
    }

    @AfterClass
    public void clearFullTextConfig() {
        repositoryService.applyFullTextSearchConfiguration(null);
    }
}
