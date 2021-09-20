/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Tests not just searchObjects, but also how this behaves after modification
 * and reindex after configuration change.
 *
 * TODO:
 *  - Config change test (reindex needed)
 *  - Test non-indexable index-only extension, where/when to show WARN about it? (config set probably)
 *  - Is there any possibility to search for whole words, start/end of the word? Currently the index
 *  should allow it (using space before the first and after the last word for this reason),
 *  but it is unclear how to indicate start/end of the word in the search string (space separates
 *  word searches combined with AND).
 */
public class SqaleSearchFullTextTest extends SqaleRepoBaseTest {

    private String user1Oid;
    private String user2Oid;
    private String user3Oid;
    private String user4Oid;
    private String task1Oid;
    private String task2Oid;
    private String shadow1Oid;

    private final String resourceOid = UUID.randomUUID().toString();

    private ItemDefinition<?> shadowAttributeDefinition;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        repositoryService.applyFullTextSearchConfiguration(
                new FullTextSearchConfigurationType(prismContext)
                        .indexed(new FullTextSearchIndexedItemsConfigurationType(prismContext)
                                .item(new ItemPathType(ObjectType.F_NAME))
                                .item(new ItemPathType(ObjectType.F_DESCRIPTION))));

        // TODO review, clean up only for fulltext search test
        // shadow, owned by user-3
        ShadowType shadow1 = new ShadowType(prismContext).name("shadow-1")
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE) // what relation is used for shadow->resource?
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent")
                .tag("tag")
                .extension(new ExtensionType(prismContext));
        addExtensionValue(shadow1.getExtension(), "string", "string-value");
        ItemName shadowAttributeName = new ItemName("https://example.com/p", "string-mv");
        ShadowAttributesHelper attributesHelper = new ShadowAttributesHelper(shadow1)
                .set(shadowAttributeName, DOMUtil.XSD_STRING, "string-value1", "string-value2");
        shadowAttributeDefinition = attributesHelper.getDefinition(shadowAttributeName);
        shadow1Oid = repositoryService.addObject(shadow1.asPrismObject(), null, result);

        UserType user1 = new UserType(prismContext).name("user-1")
                .fullName("User Name 1")
                .metadata(new MetadataType(prismContext)
                        .createChannel("create-channel")
                        .createTimestamp(asXMLGregorianCalendar(1L))
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(asXMLGregorianCalendar(2L)))
                .subtype("workerA")
                .subtype("workerC")
                .policySituation("situationA")
                .policySituation("situationC")
                .assignment(new AssignmentType(prismContext)
                        .description("assignment1 description")
                        .lifecycleState("assignment1-1")
                        .subtype("ass-subtype-2"))
                .assignment(new AssignmentType(prismContext)
                        .description("assignment2 description")
                        .lifecycleState("assignment1-2"))
                .extension(new ExtensionType(prismContext));
        // TODO extension can be full-text indexed too, but not for index-only stuff
        ExtensionType user1Extension = user1.getExtension();
        addExtensionValue(user1Extension, "string", "string-value");
        addExtensionValue(user1Extension, "int", 1);
        addExtensionValue(user1Extension, "long", 2L);
        addExtensionValue(user1Extension, "short", (short) 3);
        addExtensionValue(user1Extension, "decimal",
                new BigDecimal("12345678901234567890.12345678901234567890"));
        addExtensionValue(user1Extension, "double", Double.MAX_VALUE);
        addExtensionValue(user1Extension, "float", Float.MAX_VALUE);
        addExtensionValue(user1Extension, "boolean", true);
        addExtensionValue(user1Extension, "enum", BeforeAfterType.AFTER);
        addExtensionValue(user1Extension, "dateTime", // 2021-09-30 before noon
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_000_000_000L)));
        addExtensionValue(user1Extension, "poly", PolyString.fromOrig("poly-value"));
        addExtensionValue(user1Extension, "string-mv", "string-value1", "string-value2");
        addExtensionValue(user1Extension, "enum-mv", // nonsense semantics, sorry about it
                OperationResultStatusType.WARNING, OperationResultStatusType.SUCCESS);
        addExtensionValue(user1Extension, "int-mv", 47, 31);
        addExtensionValue(user1Extension, "string-ni", "not-indexed-item");

        ExtensionType user1AssignmentExtension = new ExtensionType(prismContext);
        user1.assignment(new AssignmentType(prismContext)
                .lifecycleState("assignment1-3-ext")
                .extension(user1AssignmentExtension));
        addExtensionValue(user1AssignmentExtension, "integer", BigInteger.valueOf(47));
        user1Oid = repositoryService.addObject(user1.asPrismObject(), null, result);

        UserType user2 = new UserType(prismContext).name("user-2")
                .description("user description with many repetitions of word user and user again")
                .subtype("workerA")
                .activation(new ActivationType(prismContext)
                        .validFrom("2021-03-01T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z"))
                .metadata(new MetadataType(prismContext)
                        .createTimestamp(asXMLGregorianCalendar(2L)))
                .extension(new ExtensionType(prismContext));
        ExtensionType user2Extension = user2.getExtension();
        addExtensionValue(user2Extension, "string", "other-value...");
        addExtensionValue(user2Extension, "dateTime", // 2021-10-01 ~15PM
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_100_000_000L)));
        addExtensionValue(user2Extension, "int", 2);
        addExtensionValue(user2Extension, "double", Double.MIN_VALUE); // positive, close to zero
        addExtensionValue(user2Extension, "float", 0f);
        addExtensionValue(user2Extension, "string-mv", "string-value2", "string-value3");
        addExtensionValue(user2Extension, "poly", PolyString.fromOrig("poly-value-user2"));
        addExtensionValue(user2Extension, "enum-mv",
                OperationResultStatusType.UNKNOWN, OperationResultStatusType.SUCCESS);
        addExtensionValue(user2Extension, "poly-mv",
                PolyString.fromOrig("poly-value1"), PolyString.fromOrig("poly-value2"));
        user2Oid = repositoryService.addObject(user2.asPrismObject(), null, result);

        UserType user3 = new UserType(prismContext).name("user-3")
                .costCenter("50")
                .policySituation("situationA")
                .assignment(new AssignmentType(prismContext)
                        .lifecycleState("ls-user3-ass1")
                        .metadata(new MetadataType(prismContext)
                                .createApproverRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT))
                        .activation(new ActivationType(prismContext)
                                .validFrom("2021-01-01T00:00:00Z"))
                        .subtype("ass-subtype-1")
                        .subtype("ass-subtype-2"))
                .linkRef(shadow1Oid, ShadowType.COMPLEX_TYPE)
                .assignment(new AssignmentType(prismContext)
                        .activation(new ActivationType(prismContext)
                                .validTo("2022-01-01T00:00:00Z")))
                .extension(new ExtensionType(prismContext));
        ExtensionType user3Extension = user3.getExtension();
        addExtensionValue(user3Extension, "int", 10);
        addExtensionValue(user3Extension, "dateTime", // 2021-10-02 ~19PM
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_200_000_000L)));
        user3Oid = repositoryService.addObject(user3.asPrismObject(), null, result);

        user4Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-4")
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
                new TaskType(prismContext).name("task-1")
                        .executionState(TaskExecutionStateType.RUNNABLE)
                        .asPrismObject(),
                null, result);
        task2Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-2")
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
                user1Oid, user2Oid, user3Oid, user4Oid, task1Oid, task2Oid, shadow1Oid);
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
    public void test122SearchAnythingContaining() throws Exception {
        searchObjectTest("with full-text containing two strings in any order",
                ObjectType.class,
                f -> f.fullText("task user"),
                user4Oid);
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
}
