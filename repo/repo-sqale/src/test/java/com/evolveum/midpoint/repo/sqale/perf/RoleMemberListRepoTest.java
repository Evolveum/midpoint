/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.perf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QRole;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * This creates specified number of roles and then assigns user to them randomly
 * so that roles with lower numbers have more users and higher have much less.
 */
public class RoleMemberListRepoTest extends SqaleRepoBaseTest {

    public static final int ROLE_MIN = 1;
    public static final int ROLE_MAX = 200;
    public static final int USER_MIN = 200001;
    public static final int USER_MAX = 300000;

    public static final int FIND_COUNT = 1000;

    private static final Random RND = new Random();

    private final List<String> roleOids = new ArrayList<>();

    @BeforeClass
    @Override
    public void initDatabase() throws Exception {
//        super.initDatabase();
        // Commented to avoid DB clearing.
        // Then we can use this to fill production/manual test database too.
    }

    @Test
    public void fillDatabase() throws SchemaException, ObjectAlreadyExistsException {
        OperationResult operationResult = createOperationResult();
        for (int roleIndex = ROLE_MIN; roleIndex <= ROLE_MAX; roleIndex++) {
            String name = String.format("role-%05d", roleIndex);
            RoleType role = new RoleType()
                    .name(PolyStringType.fromOrig(name));
            try {
                repositoryService.addObject(role.asPrismObject(), null, operationResult);
                roleOids.add(role.getOid());
                System.out.println("Created role " + name);
            } catch (ObjectAlreadyExistsException e) {
                // never mind, we'll use the existing one
                QRole r = aliasFor(QRole.class);
                MRole existingRow = selectOne(r, r.nameOrig.eq(name));
                roleOids.add(existingRow.oid.toString());
                System.out.println("Reused existing role " + name);
            }
        }

        for (int userIndex = USER_MIN; userIndex <= USER_MAX; userIndex++) {
            String name = String.format("user-%07d", userIndex);
            UserType user = new UserType()
                    .name(PolyStringType.fromOrig(name));
            addRoles(user);
            try {
                repositoryService.addObject(user.asPrismObject(), null, operationResult);
                System.out.println("Created user " + name + " with " + user.getRoleMembershipRef().size() + " roles");
            } catch (ObjectAlreadyExistsException e) {
                System.out.println("User " + name + " already exists");
            }
        }
    }

    private void addRoles(UserType user) {
        int size = roleOids.size();
        int counter = 0;
        for (String roleOid : roleOids) {
            // 2d converts size to double and also gives minuscule chance to the last role.
            double probability = (size - counter) / (2d + size);
            if (probability > RND.nextDouble()) {
                user.assignment(new AssignmentType()
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE));
                user.roleMembershipRef(roleOid, RoleType.COMPLEX_TYPE);
            }
            counter += 1;
        }
    }

    /*
    @Test
    public void test110GetUser() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.get1", "Repository getObject() -> user, 1st test");
        for (int i = 1; i <= FIND_COUNT; i++) {
            String randomName = String.format("user-%07d", RND.nextInt(BASE_USER_COUNT) + 1);
            if (i == FIND_COUNT) {
                queryRecorder.startRecording();
            }
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, users.get(randomName), null, operationResult))
                        .isNotNull();
            }
        }
        queryRecorder.stopRecording();
    }
    */
}
