/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.lang.reflect.Method;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.NS_FOO;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

/**
 * Here are the most simple tests for Query Builder.
 * More advanced tests are part of QueryInterpreterTest in repo-sql-impl-test.
 */
public class TestQueryBuilder {

    public static final QName USER_TYPE_QNAME = new QName(NS_FOO, "UserType");
    public static final QName ASSIGNMENT_TYPE_QNAME = new QName(NS_FOO, "AssignmentType");
    private static MatchingRuleRegistry matchingRuleRegistry;
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
		
		matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();
	}

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> START TEST" + getClass().getName() + "." + method.getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
    }
	
	@Test
	public void test100EmptyFilter() throws Exception{
		ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext()).build();
		ObjectQuery expected = ObjectQuery.createObjectQuery((ObjectFilter) null);
        compare(actual, expected);
    }

    @Test
    public void test110EmptyBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext()).block().endBlock().build();
        ObjectQuery expected = ObjectQuery.createObjectQuery((ObjectFilter) null);
        compare(actual, expected);
    }

    @Test
    public void test112EmptyBlocks() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .block()
                    .block()
                        .block()
                        .endBlock()
                    .endBlock()
                .endBlock()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery((ObjectFilter) null);
        compare(actual, expected);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test112BlocksLeftOpen() throws Exception{
        QueryBuilder.queryFor(UserType.class, getPrismContext())
                .block()
                .block()
                .block()
                .endBlock()
                .endBlock()
                .build();
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test114NoOpenBlocks() throws Exception{
        QueryBuilder.queryFor(UserType.class, getPrismContext())
                .block()
                .block()
                .endBlock()
                .endBlock()
                .endBlock()
                .build();
    }

    @Test
    public void test120All() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext()).all().build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(AllFilter.createAll());
        compare(actual, expected);
    }

    @Test
    public void test122AllInBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext()).block().all().endBlock().build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(AllFilter.createAll());
        compare(actual, expected);
    }

    @Test
    public void test130SingleEquals() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext()).item(UserType.F_LOCALITY).eq("Caribbean").build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean"));
        compare(actual, expected);
    }

    @Test
    public void test132SingleAnd() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .and().item(UserType.F_DESCRIPTION).eq("desc")
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                AndFilter.createAnd(
                        EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean"),
                        EqualFilter.createEqual(UserType.F_DESCRIPTION, UserType.class, getPrismContext(), "desc")
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test134SingleOrWithNot() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .or().not().item(UserType.F_DESCRIPTION).eq("desc")
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                OrFilter.createOr(
                        EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean"),
                        NotFilter.createNot(
                                EqualFilter.createEqual(UserType.F_DESCRIPTION, UserType.class, getPrismContext(), "desc")
                        )
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test136AndOrNotSequence() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .or()
                        .not().item(UserType.F_DESCRIPTION).eq("desc")
                        .and().all()
                        .and().none()
                .or()
                        .undefined()
                        .and().not().none()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                OrFilter.createOr(
                        EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean"),
                        AndFilter.createAnd(
                                NotFilter.createNot(
                                        EqualFilter.createEqual(UserType.F_DESCRIPTION, UserType.class, getPrismContext(), "desc")
                                ),
                                AllFilter.createAll(),
                                NoneFilter.createNone()
                        ),
                        AndFilter.createAnd(
                                UndefinedFilter.createUndefined(),
                                NotFilter.createNot(
                                        NoneFilter.createNone()
                                )
                        )
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test140TypeWithEquals() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                TypeFilter.createType(
                        USER_TYPE_QNAME,
                        EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean")
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test142TypeWithEqualsInBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                .block()
                    .item(UserType.F_LOCALITY).eq("Caribbean")
                .endBlock()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                TypeFilter.createType(
                        USER_TYPE_QNAME,
                        EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean")
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test144TypeWithEqualsAndAllInBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                .block()
                    .item(UserType.F_LOCALITY).eq("Caribbean")
                    .and().all()
                .endBlock()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                TypeFilter.createType(
                        USER_TYPE_QNAME,
                        AndFilter.createAnd(
                                EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, getPrismContext(), "Caribbean"),
                                AllFilter.createAll()
                        )
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test144TypeInTypeInType() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                    .type(AssignmentType.class)
                        .type(UserType.class)
                            .all()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                TypeFilter.createType(
                        USER_TYPE_QNAME,
                        TypeFilter.createType(
                                ASSIGNMENT_TYPE_QNAME,
                                TypeFilter.createType(
                                        USER_TYPE_QNAME,
                                        AllFilter.createAll()
                                )
                        )
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test146TypeAndSomething() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                    .none()
                .and().all()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                AndFilter.createAnd(
                        TypeFilter.createType(
                                USER_TYPE_QNAME,
                                NoneFilter.createNone()
                        ),
                        AllFilter.createAll()
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test148TypeEmpty() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                .block().endBlock()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                TypeFilter.createType(
                        USER_TYPE_QNAME,
                        null
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test149TypeEmptyAndAll() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .type(UserType.class)
                    .block().endBlock()
                .and().all()
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                AndFilter.createAnd(
                        TypeFilter.createType(
                                USER_TYPE_QNAME,
                                null
                        ),
                        AllFilter.createAll()
                )
        );
        compare(actual, expected);
    }


    protected void compare(ObjectQuery actual, ObjectQuery expected) {
        String exp = expected.debugDump();
        String act = actual.debugDump();
        System.out.println("Generated query:\n" + act);
        AssertJUnit.assertEquals("queries do not match", exp, act);
    }


}
