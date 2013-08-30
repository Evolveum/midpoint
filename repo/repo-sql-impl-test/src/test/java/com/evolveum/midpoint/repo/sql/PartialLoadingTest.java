package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
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
        LOGGER.info("Adding users.");
        OperationResult result = new OperationResult("{Partial loading");
        USER_OIDS = addUsers(10, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
        LOGGER.trace(result.dump());
    }

    @Test
    public void loadUserWithLinkRefs() throws Exception {
        OperationResult result = new OperationResult("Load user with link refs");

        ObjectFilter filter = SubstringFilter.createSubstring(UserType.class, prismContext,
                UserType.F_NAME, PolyStringOrigMatchingRule.NAME.getLocalPart(), "jhenry");

//        filter = EqualsFilter.createEqual(UserType.class, prismContext, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL), "asdf");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
//        options.add(SelectorOptions.create(UserType.F_RESULT,
//                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
//        options.add(SelectorOptions.create(UserType.F_METADATA,
//                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        LOGGER.info("Search users");
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options, result);
        LOGGER.info("Search users finished");

        LOGGER.info("Count users");
        repositoryService.countObjects(UserType.class, ObjectQuery.createObjectQuery(filter), result);
        LOGGER.info("Count users finished");

        LOGGER.info("Get user");
        repositoryService.getObject(UserType.class, USER_OIDS.get(3), null, result);
        LOGGER.info("Get user finished");
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
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        AssertJUnit.assertTrue(RUtil.hasToLoadPath(UserType.F_LINK_REF, options));

        options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD),
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        AssertJUnit.assertTrue(RUtil.hasToLoadPath(UserType.F_CREDENTIALS, options));
    }
}
