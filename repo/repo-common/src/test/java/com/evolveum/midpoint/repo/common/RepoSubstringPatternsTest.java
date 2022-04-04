/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This tests substring searches with potentially tricky characters (%, _, []).
 * Not all of them are tricky on all databases, e.g. PG only uses % and _ in LIKE operations,
 * but SQL Server also uses [, ] and ^ inside it.
 * All currently supported DBs have some escaping mechanism available.
 * See https://stackoverflow.com/q/712580/658826[this] for more details.
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
public class RepoSubstringPatternsTest extends AbstractRepoCommonTest {

    public static final QName POLY_STRING_ORIG_IC_MATCHING =
            new QName(PrismConstants.NS_MATCHING_RULE, "origIgnoreCase");

    private String oid1Underscore;
    private String oid2NothingSpecial;
    private String oid3Brackets;
    private String oid4Percent;
    private String oid5Caret;
    private String oid6Mix;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // we don't want superclass init, no admin user
        oid1Underscore = addUser("user_1", "User_One");
        oid2NothingSpecial = addUser("user two, not 1", "User Two");
        oid3Brackets = addUser("user [three]", "User [Three]");
        oid4Percent = addUser("user %%4%%", "User %Four%");
        oid5Caret = addUser("user ^5", "User ^Five");
        oid6Mix = addUser("user !\\[^%_][][^a] what", "User Total Overkill");
    }

    private @NotNull String addUser(String name, String fullName) throws Exception {
        return repositoryService.addObject(
                new UserType().name(name).fullName(fullName).asPrismObject(),
                null, new OperationResult("addUser"));
    }

    // region user 1, underscore
    @Test
    public void test101ContainsUnderscore1() throws Exception {
        searchUsersTest("with name containing underscore",
                f -> f.item(UserType.F_NAME).contains("_"),
                oid1Underscore, oid6Mix);
    }

    @Test
    public void test102ContainsUnderscore2() throws Exception {
        searchUsersTest("with name containing %_ substring",
                f -> f.item(UserType.F_NAME).contains("%_"),
                oid6Mix);
    }

    @Test
    public void test103ContainsUnderscore3() throws Exception {
        searchUsersTest("with name containing _1 substring",
                f -> f.item(UserType.F_NAME).contains("_1"),
                oid1Underscore);
    }

    @Test
    public void test110ContainsUser1Name() throws Exception {
        searchUsersTest("with name containing user_1 substring",
                f -> f.item(UserType.F_NAME).contains("user_1"),
                oid1Underscore);
    }

    @Test
    public void test111StartsWithUser1Name() throws Exception {
        searchUsersTest("with name starting with user_1",
                f -> f.item(UserType.F_NAME).startsWith("user_1"),
                oid1Underscore);
    }

    @Test
    public void test112EndsWithUser1Name() throws Exception {
        searchUsersTest("with name ending with user_1",
                f -> f.item(UserType.F_NAME).endsWith("user_1"),
                oid1Underscore);
    }

    @Test
    public void test113ContainsUser1NameIgnoreCase() throws Exception {
        searchUsersTest("with name containing USER_1 substring, ignore case",
                f -> f.item(UserType.F_NAME).contains("USER_1").matching(POLY_STRING_ORIG_IC_MATCHING),
                oid1Underscore);
    }
    // endregion

    // region user 2, nothing special
    @Test
    public void test200ContainsUser2Name() throws Exception {
        searchUsersTest("with name containing 'user two, not 1' substring",
                f -> f.item(UserType.F_NAME).contains("user two, not 1"),
                oid2NothingSpecial);
    }

    @Test
    public void test201StartsWithUser2Name() throws Exception {
        searchUsersTest("with name starting with 'user two, not 1'",
                f -> f.item(UserType.F_NAME).startsWith("user two, not 1"),
                oid2NothingSpecial);
    }

    @Test
    public void test202EndsWithUser2Name() throws Exception {
        searchUsersTest("with name ending with 'user two, not 1'",
                f -> f.item(UserType.F_NAME).endsWith("user two, not 1"),
                oid2NothingSpecial);
    }

    @Test
    public void test203ContainsUser2NameIgnoreCase() throws Exception {
        searchUsersTest("with name containing 'USER TWO, NOT 1' substring, ignore case",
                f -> f.item(UserType.F_NAME).contains("USER TWO, NOT 1").matching(POLY_STRING_ORIG_IC_MATCHING),
                oid2NothingSpecial);
    }
    // endregion

    // region user 3, brackets
    @Test
    public void test301SubstringWithBrackets1() throws Exception {
        searchUsersTest("with name containing [ substring",
                f -> f.item(UserType.F_NAME).contains("["),
                oid3Brackets, oid6Mix);
    }

    @Test
    public void test302SubstringWithBrackets2() throws Exception {
        searchUsersTest("with name containing ] substring",
                f -> f.item(UserType.F_NAME).contains("]"),
                oid3Brackets, oid6Mix);
    }

    @Test
    public void test303SubstringWithBrackets3() throws Exception {
        searchUsersTest("with name containing [e] substring",
                f -> f.item(UserType.F_NAME).contains("[e]"));
    }

    @Test
    public void test304SubstringWithBrackets4() throws Exception {
        searchUsersTest("with name containing [^a] substring",
                f -> f.item(UserType.F_NAME).contains("[^a]"),
                oid6Mix);
    }

    @Test
    public void test310ContainsUser3Name() throws Exception {
        searchUsersTest("with name containing 'user [three]' substring",
                f -> f.item(UserType.F_NAME).contains("user [three]"),
                oid3Brackets);
    }

    @Test
    public void test311StartsWithUser3Name() throws Exception {
        searchUsersTest("with name starting with 'user [three]'",
                f -> f.item(UserType.F_NAME).startsWith("user [three]"),
                oid3Brackets);
    }

    @Test
    public void test312EndsWithUser3Name() throws Exception {
        searchUsersTest("with name ending with 'user [three]'",
                f -> f.item(UserType.F_NAME).endsWith("user [three]"),
                oid3Brackets);
    }

    @Test
    public void test313ContainsUser3NameIgnoreCase() throws Exception {
        searchUsersTest("with name containing 'USER [THREE]' substring, ignore case",
                f -> f.item(UserType.F_NAME).contains("USER [THREE]").matching(POLY_STRING_ORIG_IC_MATCHING),
                oid3Brackets);
    }
    // endregion

    // region user 4, percent
    @Test
    public void test401SubstringWithPercent1() throws Exception {
        searchUsersTest("with name containing percent",
                f -> f.item(UserType.F_NAME).contains("%"),
                oid4Percent, oid6Mix);
    }

    @Test
    public void test402SubstringWithPercent2() throws Exception {
        searchUsersTest("with name containing %%",
                f -> f.item(UserType.F_NAME).contains("%%"),
                oid4Percent);
    }

    @Test
    public void test410ContainsUser4Name() throws Exception {
        searchUsersTest("with name containing 'user %%4%%' substring",
                f -> f.item(UserType.F_NAME).contains("user %%4%%"),
                oid4Percent);
    }

    @Test
    public void test411StartsWithUser4Name() throws Exception {
        searchUsersTest("with name starting with 'user %%4%%'",
                f -> f.item(UserType.F_NAME).startsWith("user %%4%%"),
                oid4Percent);
    }

    @Test
    public void test412EndsWithUser4Name() throws Exception {
        searchUsersTest("with name ending with 'user %%4%%'",
                f -> f.item(UserType.F_NAME).endsWith("user %%4%%"),
                oid4Percent);
    }

    @Test
    public void test413ContainsUser4NameIgnoreCase() throws Exception {
        searchUsersTest("with name containing 'USER %%4%%' substring, ignore case",
                f -> f.item(UserType.F_NAME).contains("USER %%4%%").matching(POLY_STRING_ORIG_IC_MATCHING),
                oid4Percent);
    }
    // endregion

    // region user 5, caret
    @Test
    public void test501SubstringWithCaret1() throws Exception {
        searchUsersTest("with name containing caret",
                f -> f.item(UserType.F_NAME).contains("^"),
                oid5Caret, oid6Mix);
    }

    @Test
    public void test502SubstringWithCaret2() throws Exception {
        searchUsersTest("with name containing ^a",
                f -> f.item(UserType.F_NAME).contains("^a"),
                oid6Mix);
    }

    @Test
    public void test503SubstringWithCaret3() throws Exception {
        // unescaped can mean "contains any character that is not 'a'" (SQL Server)
        searchUsersTest("with name containing [^a]",
                f -> f.item(UserType.F_NAME).contains("[^a]"),
                oid6Mix);
    }

    @Test
    public void test504SubstringWithCaret4() throws Exception {
        // unescaped can mean "contains any character that is not 'a'" (SQL Server)
        searchUsersTest("with name containing [^x]",
                f -> f.item(UserType.F_NAME).contains("[^x]"));
    }

    @Test
    public void test510ContainsUser4Name() throws Exception {
        searchUsersTest("with name containing 'user ^5' substring",
                f -> f.item(UserType.F_NAME).contains("user ^5"),
                oid5Caret);
    }

    @Test
    public void test511StartsWithUser4Name() throws Exception {
        searchUsersTest("with name starting with 'user ^5'",
                f -> f.item(UserType.F_NAME).startsWith("user ^5"),
                oid5Caret);
    }

    @Test
    public void test512EndsWithUser4Name() throws Exception {
        searchUsersTest("with name ending with 'user ^5'",
                f -> f.item(UserType.F_NAME).endsWith("user ^5"),
                oid5Caret);
    }

    @Test
    public void test513ContainsUser4NameIgnoreCase() throws Exception {
        searchUsersTest("with name containing 'USER ^5' substring, ignore case",
                f -> f.item(UserType.F_NAME).contains("USER ^5").matching(POLY_STRING_ORIG_IC_MATCHING),
                oid5Caret);
    }
    // endregion

    @Test
    public void test601ContainsMixName() throws Exception {
        searchUsersTest("with name containing 'user !\\[^%_][][^a] what'",
                f -> f.item(UserType.F_NAME).contains("user !\\[^%_][][^a] what"),
                oid6Mix);
    }

    @SuppressWarnings("UnusedReturnValue")
    private SearchResultList<UserType> searchUsersTest(String description,
            Function<S_FilterEntryOrEmpty, S_FilterExit> filter,
            String... expectedOids)
            throws SchemaException {
        when("searching for user(s) " + description);
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = filter.apply(prismContext.queryFor(UserType.class)).build();

        SearchResultList<UserType> result = repositoryService
                .searchObjects(UserType.class, query, null, operationResult)
                .map(p -> p.asObjectable());

        then("User(s) " + description + " are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(expectedOids);
        return result;
    }
}
