package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PartialLoadingTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(PartialLoadingTest.class);

    private List<String> USER_OIDS;

    @Override
    public void initSystem() throws Exception {
        OperationResult result = new OperationResult("Partial loading");
        USER_OIDS = addUsers(1, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void searchUserNullOptions() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = null;

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any, operation_result, metadata
        AssertJUnit.assertEquals(6, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search - 9 selects
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUser() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any, operation_result, metadata
        AssertJUnit.assertEquals(6, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUserDefaultPath() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUserWithLinkRefs() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any
        //link ref are SELECTs for each returned user
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, null, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUserResult() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_RESULT,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any, operation_result
        AssertJUnit.assertEquals(5, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, null, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUserMetadata() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_METADATA,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any metadata
        AssertJUnit.assertEquals(5, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, null, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    @Test
    public void searchUserMetadataAndResult() throws Exception {
        ObjectFilter filter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext,
                PolyStringOrigMatchingRule.NAME, "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_RESULT,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(UserType.F_METADATA,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        String mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, options, false);
        //join with container, object, focus, any, operation_result, metadata
        AssertJUnit.assertEquals(6, StringUtils.countMatches(mainSql, "join"));

        mainSql = getInterpreted(ObjectQuery.createObjectQuery(filter), UserType.class, null, true);
        AssertJUnit.assertEquals(4, StringUtils.countMatches(mainSql, "join"));

        LOGGER.debug(">>> REAL SEARCH");
        //real search
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options,
                new OperationResult("asdf"));
    }

    private String getInterpreted(ObjectQuery query, Class<? extends ObjectType> type,
                                  Collection<SelectorOptions<GetOperationOptions>> options, boolean count)
            throws Exception {
        Session session = open();
        try {
            QueryInterpreter interpreter = new QueryInterpreter();
            Criteria criteria = interpreter.interpret(query, type, options, prismContext, count, session);

            String sql = HibernateToSqlTranslator.toSql(criteria);
            LOGGER.debug(sql);

            return sql;
        } finally {
            close(session);
        }
    }

    private String getInterpretedGet(String oid, Class<? extends ObjectType> type,
                                     Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
        Session session = open();
        try {
            QueryInterpreter interpreter = new QueryInterpreter();
            Criteria criteria = interpreter.interpretGet(oid, type, options, prismContext, session);

            return HibernateToSqlTranslator.toSql(criteria);
        } finally {
            close(session);
        }
    }

    private List<String> addUsers(int count, OperationResult result) throws Exception {
        List<String> oids = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            UserType user = new UserType();
            user.setName(new PolyStringType("jhenry" + i));
            user.setFullName(new PolyStringType("John Henry" + i));
            user.setGivenName(new PolyStringType("John"));
            user.setFamilyName(new PolyStringType("Henry" + i));

            oids.add(repositoryService.addObject(user.asPrismObject(), null, result));
        }

        return oids;
    }

    @Test
    public void testOptionsResolution() throws Exception {
        //sample
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_LINK_REF, options));

        //sample
        options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD),
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_CREDENTIALS, options));

        //sample
        options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_RESULT, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_METADATA, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_ASSIGNMENT, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_LINK_REF, options));

        //sample
        options = null;
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_RESULT, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_METADATA, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_ASSIGNMENT, options));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(UserType.F_LINK_REF, options));
    }
}
