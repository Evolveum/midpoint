package com.evolveum.midpoint.prism.query.lang;

import java.io.File;
import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.impl.query.lang.PrismQueryLanguageParser;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestBasicQueryConversions extends AbstractPrismTest {

    public static final File FILE_USER_JACK_FILTERS =
            new File(PrismInternalTestUtil.COMMON_DIR_XML, "user-jack-filters.xml");

    private static final MatchingRuleRegistry MATCHING_RULE_REGISTRY =
            MatchingRuleRegistryFactory.createRegistry();

    private PrismObject<UserType> parseUserJack() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(FILE_USER_JACK_FILTERS);
    }

    @Test
    public void basicAndFilter() throws SchemaException, IOException {


        PrismQueryLanguageParser parser = new PrismQueryLanguageParser(PrismTestUtil.getPrismContext());


        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .buildFilter();


        ObjectFilter dslFilter = parser.parseQuery(UserType.class, "givenName =[stringIgnoreCase] \"Jack\" and fullName contains \"arr\"");

        boolean match = ObjectQuery.match(user, dslFilter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);

    }





}
