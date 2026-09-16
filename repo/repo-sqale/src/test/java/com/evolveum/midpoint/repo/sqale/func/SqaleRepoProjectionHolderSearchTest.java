/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.util.UUID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCase;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QProjectionHolder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Tests searching of {@link ProjectionHolderType}, the abstract supertype of foci and cases.
 * The m_projection_holder table is the common parent of m_focus and m_case tables.
 */
public class SqaleRepoProjectionHolderSearchTest extends SqaleRepoBaseTest {

    private final String resourceOid = UUID.randomUUID().toString();

    private String shadow1Oid;
    private String shadow2Oid;
    private String user1Oid; // user with linkRef to shadow-1
    private String user2Oid; // user without projections
    private String case1Oid; // case with linkRef to shadow-1
    private String case2Oid; // case without projections
    private String task1Oid; // must not be returned by projection holder searches

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        shadow1Oid = repositoryService.addObject(
                new ShadowType().name("shadow-1")
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .asPrismObject(),
                null, result);
        shadow2Oid = repositoryService.addObject(
                new ShadowType().name("shadow-2")
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .asPrismObject(),
                null, result);

        user1Oid = repositoryService.addObject(
                new UserType().name("user-1")
                        .linkRef(shadow1Oid, ShadowType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        user2Oid = repositoryService.addObject(
                new UserType().name("user-2")
                        .linkRef(shadow2Oid, ShadowType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);

        case1Oid = repositoryService.addObject(
                new CaseType().name("case-1")
                        .state("open")
                        .linkRef(shadow1Oid, ShadowType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        case2Oid = repositoryService.addObject(
                new CaseType().name("case-2")
                        .state("created")
                        .asPrismObject(),
                null, result);

        task1Oid = repositoryService.addObject(
                new TaskType().name("task-1")
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100SearchProjectionHolderByLinkRef() throws Exception {
        searchObjectTest("having projection (linkRef) to shadow-1", ProjectionHolderType.class,
                f -> f.item(ProjectionHolderType.F_LINK_REF).ref(shadow1Oid),
                user1Oid, case1Oid);
    }

    @Test
    public void test101SearchFocusByLinkRefExcludesCase() throws Exception {
        // regression guard: focus search must not return cases
        searchObjectTest("having projection (linkRef) to shadow-1", UserType.class,
                f -> f.item(ProjectionHolderType.F_LINK_REF).ref(shadow1Oid),
                user1Oid);
    }

    @Test
    public void test102SearchCaseByLinkRef() throws Exception {
        searchObjectTest("having projection (linkRef) to shadow-1", CaseType.class,
                f -> f.item(ProjectionHolderType.F_LINK_REF).ref(shadow1Oid),
                case1Oid);
    }

    @Test
    public void test103SearchProjectionHolderByLinkRefToOtherShadow() throws Exception {
        searchObjectTest("having projection (linkRef) to shadow-2", ProjectionHolderType.class,
                f -> f.item(ProjectionHolderType.F_LINK_REF).ref(shadow2Oid),
                user2Oid);
    }

    @Test
    public void test104SearchProjectionHolderWithoutLinkRef() throws Exception {
        searchObjectTest("without projections (linkRef)", ProjectionHolderType.class,
                f -> f.item(ProjectionHolderType.F_LINK_REF).isNull(),
                case2Oid);
    }

    @Test
    public void test110SearchAllProjectionHolders() throws Exception {
        searchObjectTest("", ProjectionHolderType.class,
                f -> f,
                user1Oid, user2Oid, case1Oid, case2Oid);
    }

    @Test
    public void test111SearchByProjectionHolderTypeFilter() throws Exception {
        searchObjectTest("matching the projection holder type filter", ObjectType.class,
                f -> f.type(ProjectionHolderType.class),
                user1Oid, user2Oid, case1Oid, case2Oid);
    }

    @Test
    public void test120ProjectionHolderTableCountsFocusPlusCase() {
        and("the m_projection_holder table contains exactly foci and cases");
        long projectionHolders = count(QProjectionHolder.CLASS);
        long foci = count(QFocus.CLASS);
        long cases = count(new QCase("c"));

        assertThat(projectionHolders)
                .as("projection holders count")
                .isEqualTo(foci + cases)
                .isEqualTo(4);
    }

    @Test
    public void test121AssignmentHolderSearchStillSeesFociAndCases() {
        and("m_assignment_holder hierarchy (via m_projection_holder) still contains foci and cases");
        long assignmentHolders = count(QAssignmentHolder.CLASS);
        assertThat(assignmentHolders)
                .as("assignment holders count")
                .isEqualTo(5);
    }

    @Test
    public void test130ReadProjectionHolderObjects() throws Exception {
        when("searching all projection holders");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ProjectionHolderType> result = searchObjects(ProjectionHolderType.class,
                prismContext.queryFor(ProjectionHolderType.class).build(),
                operationResult);

        then("both users and cases are returned as concrete objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(4)
                .anySatisfy(o -> {
                    assertThat(o).isInstanceOf(UserType.class);
                    assertThat(o.getOid()).isEqualTo(user1Oid);
                })
                .anySatisfy(o -> {
                    assertThat(o).isInstanceOf(CaseType.class);
                    assertThat(o.getOid()).isEqualTo(case1Oid);
                });
    }
}
