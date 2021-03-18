/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.assertj.core.api.Assertions.assertThat;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.Jsonb;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Contains a few tests doing stuff all over the repository including a few lower level
 * (sub-repo-API) tests around Querydsl and our adaptation of it.
 */
public class SqaleRepoSmokeTest extends SqaleRepoBaseTest {

    private String sanityUserOid;

    @BeforeClass
    public void init() {
        // TODO remove later, just for initial debugging
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SqlLogger.class))
                .setLevel(ch.qos.logback.classic.Level.TRACE);
    }

    @Test
    public void test000Sanity() {
        assertThat(repositoryService).isNotNull();

        // DB should be empty
        assertCount(QObject.CLASS, 0);
        assertCount(QContainer.CLASS, 0);

        // selects check also mapping to M-classes
        assertThat(select(aliasFor(QObject.CLASS))).isEmpty();
        assertThat(select(aliasFor(QContainer.CLASS))).isEmpty();
    }

    @Test
    public void test100AddObject() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        UserType userType = new UserType(prismContext)
                .name("sanity-user");
        sanityUserOid = repositoryService.addObject(userType.asPrismObject(), null, result);

        assertThat(sanityUserOid).isNotNull();
        assertResult(result);
    }

    @Test(enabled = false) // TODO deleteObject not implemented yet
    public void test800DeleteObject() throws ObjectNotFoundException {
        OperationResult result = createOperationResult();

        DeleteObjectResult deleteResult =
                repositoryService.deleteObject(UserType.class, sanityUserOid, result);

        assertThat(deleteResult).isNotNull();
        assertResult(result);
    }

    // region low-level tests
    @Test
    public void test900WorkingWithPgArraysJsonbAndBytea() {
        QUser u = aliasFor(QUser.class);
        MUser user = new MUser();

        String userName = "user" + getTestNumber();
        setName(user, userName);
        user.policySituations = new Integer[] { 1, 2 };
        user.subtypes = new String[] { "subtype1", "subtype2" };
        user.ext = new Jsonb("{\"key\" : \"value\",\n\"number\": 47} "); // more whitespaces/lines
        user.photo = new byte[] { 0, 1, 0, 1 };
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            jdbcSession.newInsert(u).populate(user).execute();
        }

        MUser row = selectOne(u, u.nameNorm.eq(userName));
        assertThat(row.policySituations).contains(1, 2);
        assertThat(row.subtypes).contains("subtype1", "subtype2");
        assertThat(row.ext.value).isEqualTo("{\"key\": \"value\", \"number\": 47}"); // normalized
        // byte[] is used for fullObject, there is no chance to miss a problem with it
        assertThat(row.photo).hasSize(4);

        // setting NULLs
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            jdbcSession.newUpdate(u)
                    .setNull(u.policySituations)
                    .set(u.subtypes, (String[]) null) // this should do the same
                    .setNull(u.ext)
                    .setNull(u.photo)
                    .where(u.oid.eq(row.oid))
                    .execute();
        }

        row = selectOne(u, u.nameNorm.eq(userName));
        assertThat(row.policySituations).isNull();
        assertThat(row.subtypes).isNull();
        assertThat(row.ext).isNull();
        // but we never set fullObject to null, so this is a good test for doing so with byte[]
        assertThat(row.photo).isNull();
    }
    // endregion
}
