package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PartialLoadingTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(PartialLoadingTest.class);

    @Test
    public void loadUserWithLinkRefs() throws Exception {
        LOGGER.info("Adding users.");
        OperationResult result = new OperationResult("parial loading");
        addUsers(10, result);

        ObjectFilter filter = SubstringFilter.createSubstring(UserType.class, prismContext,
                UserType.F_NAME, PolyStringOrigMatchingRule.NAME.getLocalPart(), "jhenry");

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF, GetOperationOptions.createResolve()));

        LOGGER.info("Search users");
        repositoryService.searchObjects(UserType.class, ObjectQuery.createObjectQuery(filter), options, result);
        LOGGER.info("Search users finished");
    }

    private void addUsers(int count, OperationResult result) throws Exception {
        for (int i = 0; i < count; i++) {
            UserType user = new UserType();
            user.setName(new PolyStringType("jhenry" + i));
            user.setFullName(new PolyStringType("John Henry" + i));
            user.setGivenName(new PolyStringType("John"));
            user.setFamilyName(new PolyStringType("Henry" + i));

            repositoryService.addObject(user.asPrismObject(), null, result);
        }
    }
}
