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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIConversion;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIConversion.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getSchemaRegistry;

/**
 * Here are the most simple tests for Query Builder.
 * More advanced tests are part of QueryInterpreterTest in repo-sql-impl-test.
 *
 * These tests are strict in the sense they check the exact structure of created queries.
 * Repo tests check just the HQL outcome.
 */
public class TestQueryBuilder {
	
	private static transient Trace LOGGER = TraceManager.getTrace(TestQueryBuilder.class);

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

    @Test(expectedExceptions = IllegalStateException.class)
    public void test113BlocksLeftOpen() throws Exception{
        QueryBuilder.queryFor(UserType.class, getPrismContext())
                .block()
                .block()
                .block()
                .endBlock()
                .endBlock()
                .build();
    }

    @Test(expectedExceptions = IllegalStateException.class)
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
        ObjectQuery expected = ObjectQuery.createObjectQuery(createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"));
        compare(actual, expected);
        
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, getPrismContext())
        		.item(UserType.F_LOCALITY)
        			.eq("Caribbean")
        		.and()
        			.item(UserType.F_GIVEN_NAME)
        				.eq("asd")
        		.and()
        			.block().item(UserType.F_FAMILY_NAME)
        				.eq("asdasd")
        				
        				.or()
            			.item(UserType.F_FAMILY_NAME)
            				.eq("asdasd")
            				
            				.and()
            					.block()
            						.item(UserType.F_FAMILY_NAME).gt("123")
            						.and().item(UserType.F_LOCALITY).le("123")
            						.or().item(UserType.F_DESCRIPTION).isNull()
            					.endBlock()
            				.and().item(UserType.F_GIVEN_NAME).eq("123")
            		.endBlock()
            			.and().item(UserType.F_FULL_NAME).contains("123").matching(PolyStringNormMatchingRule.NAME)
            				.build();
        LOGGER.info("Query:\n {}", query.debugDump());
    }

    @Test
    public void test132SingleAnd() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .and().item(UserType.F_DESCRIPTION).eq("desc")
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                AndFilter.createAnd(
                        createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"),
                        createEqual(UserType.F_DESCRIPTION, UserType.class, null, "desc")
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
                        createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"),
                        NotFilter.createNot(
                                createEqual(UserType.F_DESCRIPTION, UserType.class, null, "desc")
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
                        createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"),
                        AndFilter.createAnd(
                                NotFilter.createNot(
                                        createEqual(UserType.F_DESCRIPTION, UserType.class, null, "desc")
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
                        createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean")
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
                        createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean")
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
                                createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"),
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

    @Test
    public void test150ExistsWithEquals() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).startsWith("desc1")
                .build();
        ComplexTypeDefinition assCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AssignmentType.class);
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                ExistsFilter.createExists(
                        new ItemPath(UserType.F_ASSIGNMENT),
                        userPcd,
                        SubstringFilter.createSubstring(
                                new ItemPath(AssignmentType.F_DESCRIPTION),
                                assCtd.findPropertyDefinition(AssignmentType.F_DESCRIPTION),
                                getPrismContext(),
                                null, "desc1", true, false)
                )
                );
        compare(actual, expected);
    }

    @Test
    public void test151ExistsWithEquals2() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_NOTE).endsWith("DONE.")
                .build();
        ComplexTypeDefinition assCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AssignmentType.class);
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                ExistsFilter.createExists(
                        new ItemPath(UserType.F_ASSIGNMENT),
                        userPcd,
                        SubstringFilter.createSubstring(
                                new ItemPath(AssignmentType.F_NOTE),
                                assCtd.findPropertyDefinition(AssignmentType.F_NOTE),
                                getPrismContext(),
                                null, "DONE.", false, true)
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test152ExistsWithEqualsInBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .exists(UserType.F_ASSIGNMENT)
                .block()
                    .item(AssignmentType.F_NOTE).endsWith("DONE.")
                .endBlock()
                .build();
        ComplexTypeDefinition assCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AssignmentType.class);
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                ExistsFilter.createExists(
                        new ItemPath(UserType.F_ASSIGNMENT),
                        userPcd,
                        SubstringFilter.createSubstring(
                                new ItemPath(AssignmentType.F_NOTE),
                                assCtd.findPropertyDefinition(AssignmentType.F_NOTE),
                                getPrismContext(),
                                null, "DONE.", false, true)
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test154ExistsWithEqualsAndAllInBlock() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .exists(UserType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_NOTE).endsWith("DONE.")
                    .and().all()
                .endBlock()
                .build();
        ComplexTypeDefinition assCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AssignmentType.class);
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                ExistsFilter.createExists(
                        new ItemPath(UserType.F_ASSIGNMENT),
                        userPcd,
                        AndFilter.createAnd(
                                SubstringFilter.createSubstring(
                                        new ItemPath(AssignmentType.F_NOTE),
                                        assCtd.findPropertyDefinition(AssignmentType.F_NOTE),
                                        getPrismContext(),
                                        null, "DONE.", false, true
                                ),
                                AllFilter.createAll()
                        )
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test156ExistsAndSomething() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .exists(UserType.F_ASSIGNMENT)
                    .none()
                .and().all()
                .build();

        ComplexTypeDefinition assCtd = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AssignmentType.class);
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                AndFilter.createAnd(
                        ExistsFilter.createExists(
                                new ItemPath(UserType.F_ASSIGNMENT),
                                userPcd,
                                NoneFilter.createNone()
                        ),
                        AllFilter.createAll()
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test200OrderByName() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .asc(UserType.F_NAME)
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                ObjectPaging.createPaging(UserType.F_NAME, OrderDirection.ASCENDING)
        );
        compare(actual, expected);
    }

    @Test
    public void test210OrderByNameAndId() throws Exception{
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq("Caribbean")
                .asc(UserType.F_NAME)
                .desc(PrismConstants.T_ID)
                .build();
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.addOrderingInstruction(UserType.F_NAME, OrderDirection.ASCENDING);
        paging.addOrderingInstruction(PrismConstants.T_ID, OrderDirection.DESCENDING);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                createEqual(UserType.F_LOCALITY, UserType.class, null, "Caribbean"),
                paging);
        compare(actual, expected);
    }

    @Test
    public void test300EqualItem() throws Exception {
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(UserType.F_LOCALITY).eq().item(UserType.F_NAME)
                .build();
        PrismContainerDefinition userPcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                EqualFilter.createEqual(
                        new ItemPath(UserType.F_LOCALITY),
                        userPcd.findPropertyDefinition(UserType.F_LOCALITY),
                        null,
                        new ItemPath(UserType.F_NAME),
                        null
                )
        );
        compare(actual, expected);
    }

    @Test
    public void test310LessThanItem() throws Exception {
        PrismObjectDefinition userDef = getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition nameDef = userDef.findPropertyDefinition(UserType.F_NAME);
        PrismPropertyDefinition localityDef = userDef.findPropertyDefinition(UserType.F_LOCALITY);
        ObjectQuery actual = QueryBuilder.queryFor(UserType.class, getPrismContext())
                .item(new ItemPath(UserType.F_LOCALITY), localityDef)
                    .le()
                        .item(new ItemPath(UserType.F_NAME), nameDef)
                .build();
        ObjectQuery expected = ObjectQuery.createObjectQuery(
                LessFilter.createLess(
                        new ItemPath(UserType.F_LOCALITY),
                        localityDef,
                        new ItemPath(UserType.F_NAME),
                        nameDef,
                        true
                )
        );
        compare(actual, expected);
    }

    protected void compare(ObjectQuery actual, ObjectQuery expected) {
        String exp = expected.debugDump();
        String act = actual.debugDump();
        
        LOGGER.info("Generated query:\n {}", act);
        System.out.println("Generated query:\n" + act);
        AssertJUnit.assertEquals("queries do not match", exp, act);
    }

    private <C extends Containerable, T> EqualFilter<T> createEqual(QName propertyName, Class<C> type, QName matchingRule, T realValue) {
        return createEqual(new ItemPath(propertyName), type, matchingRule, realValue);
    }

    private <C extends Containerable, T> EqualFilter<T> createEqual(ItemPath propertyPath, Class<C> type, QName matchingRule, T realValue) {
        PrismPropertyDefinition<T> propertyDefinition = (PrismPropertyDefinition) FilterUtils.findItemDefinition(propertyPath, type, getPrismContext());
        return EqualFilter.createEqual(propertyPath, propertyDefinition, matchingRule, getPrismContext(), realValue);
    }
}
