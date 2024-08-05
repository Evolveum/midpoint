/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.sql.PrimaryKey;
import com.querydsl.sql.SQLQuery;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtString;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtString;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.repo.sql.testing.TestQueryListener;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.test.util.TestReportUtil;
import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class BaseSQLRepoTest extends AbstractSpringTest
        implements InfraTestMixin {

    static final File FOLDER_BASE = new File("./src/test/resources");

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");

    protected static final String NS_EXT = "http://example.com/p";

    static final ItemName EXT_HIDDEN1 = new ItemName(NS_EXT, "hidden1");
    static final ItemName EXT_HIDDEN2 = new ItemName(NS_EXT, "hidden2");
    static final ItemName EXT_HIDDEN3 = new ItemName(NS_EXT, "hidden3");
    static final ItemName EXT_VISIBLE = new ItemName(NS_EXT, "visible");
    static final ItemName EXT_VISIBLE_SINGLE = new ItemName(NS_EXT, "visibleSingle");

    static final ItemName EXT_LOOT = new ItemName(NS_EXT, "loot");
    static final ItemName EXT_WEAPON = new ItemName(NS_EXT, "weapon");
    static final ItemName EXT_SHIP_NAME = new ItemName(NS_EXT, "shipName");

    static final ItemName ATTR_GROUP_NAME = new ItemName(NS_RI, "groupName");
    static final ItemName ATTR_MEMBER = new ItemName(NS_RI, "member");
    static final ItemName ATTR_MANAGER = new ItemName(NS_RI, "manager");

    // We want existing bean "repositoryService" but downcast to access configuration, etc.
    // No, we don't want @Repository or anything else in ctx*.xml that creates SRSI bean twice.
    @Autowired
    @Qualifier("repositoryService")
    protected SqlRepositoryServiceImpl sqlRepositoryService;

    @Autowired protected RepositoryService repositoryService;
    @Autowired protected BaseHelper baseHelper;
    @Autowired protected AuditService auditService;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaService schemaService;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected EntityManagerFactory factory;
    @Autowired protected ExtItemDictionary extItemDictionary;
    @Autowired protected Protector protector;
    @Autowired protected TestQueryListener queryListener;

    protected boolean verbose = false;

    @BeforeSuite
    public void prismContextForTestSuite() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    public EntityManagerFactory getFactory() {
        return factory;
    }

    public void setFactory(EntityManagerFactory factory) {
        this.factory = factory;
    }

    private volatile boolean initSystemExecuted = false;

    @PostConstruct
    public void initializeTestClass() throws Exception {
        if (initSystemExecuted) {
            logger.trace("initSystem: already called for class {} - IGNORING", getClass().getName());
            return;
        }

        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
        initSystemExecuted = true;
        initSystem();
    }

    /** Called only by performance tests. */
    @Override
    public TestMonitor createTestMonitor() {
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        queryListener.clear();
        return super.createTestMonitor()
                .addReportCallback(TestReportUtil::reportGlobalPerfData)
                .addReportCallback(SqlRepoTestUtil.reportCallbackQuerySummary(queryListener))
                .addReportCallback(SqlRepoTestUtil.reportCallbackQueryList(queryListener));
    }

    protected boolean isUsingH2() {
        return baseHelper.getConfiguration().isUsingH2();
    }

    public void initSystem() throws Exception {
    }

    protected EntityManager open() {
        EntityManager em = getFactory().createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    protected void close(EntityManager em) {
        if (!em.getTransaction().getRollbackOnly()) {
            em.getTransaction().commit();
        }
        em.close();
    }

    String hqlToSql(String hql) {
        //return HibernateToSqlTranslator.toSql(factory, hql);
        throw new UnsupportedOperationException("Not migrated to Hibernate 6");
    }

    protected <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getObject");
        PrismObject<O> object = repositoryService.getObject(type, oid, null, result);
        assertThatOperationResult(result).isSuccess();
        return object;
    }

    protected <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException {
        return prismContext.deltaFor(objectClass);
    }

    protected GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return schemaService.getOperationOptionsBuilder();
    }

    protected ItemFactory itemFactory() {
        return prismContext.itemFactory();
    }

    @NotNull
    ObjectReference createRepoRef(QName type, String oid) {
        return RObjectReference.copyFromJAXB(
                createRef(type, oid, SchemaConstants.ORG_DEFAULT),
                new RObjectReference<>(),
                relationRegistry);
    }

    ObjectReferenceType createRef(QName type, String oid) {
        return createRef(type, oid, null);
    }

    ObjectReferenceType createRef(QName type, String oid, QName relation) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(type);
        ref.setOid(oid);
        ref.setRelation(relation);
        return ref;
    }

    void assertReferences(Collection<ObjectReference> collection, ObjectReference... expected) {
        AssertJUnit.assertEquals(expected.length, collection.size());

        for (ObjectReference ref : collection) {
            boolean found = false;
            for (ObjectReference exp : expected) {
                if (Objects.equals(exp.getRelation(), ref.getRelation())
                        && Objects.equals(exp.getTargetOid(), ref.getTargetOid())
                        && Objects.equals(exp.getTargetType(), ref.getTargetType())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                AssertJUnit.fail("Reference doesn't match " + ref);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    REmbeddedReference createEmbeddedRepoRef(QName type, String oid) {
        return REmbeddedReference.fromJaxb(createRef(type, oid, SchemaConstants.ORG_DEFAULT), new REmbeddedReference(),
                relationRegistry);
    }

    @SuppressWarnings("SameParameterValue")
    RExtItem createOrFindExtensionItemDefinition(Class<? extends ObjectType> type, ItemName itemName) {
        PrismContainerDefinition<?> extDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(type).getExtensionDefinition();
        PrismPropertyDefinition<Object> propertyDefinition = extDef.findPropertyDefinition(itemName);
        assertNotNull("Definition of extension item " + itemName + " was not found", propertyDefinition);
        return extItemDictionary.createOrFindItemDefinition(propertyDefinition, false);
    }

    void assertExtension(RObject object, RExtItem item, String... expectedValues) {
        Set<String> realValues = object.getStrings().stream()
                .filter(extString -> Objects.equals(extString.getItemId(), item.getId()))
                .map(ROExtString::getValue)
                .collect(Collectors.toSet());
        assertEquals("Wrong values of object extension item " + item.getName(), new HashSet<>(Arrays.asList(expectedValues)), realValues);
    }

    void assertExtension(RAssignment assignment, RExtItem item, String... expectedValues) {
        assertNotNull(assignment.getExtension());
        Set<String> realValues = assignment.getExtension().getStrings().stream()
                .filter(extString -> Objects.equals(extString.getItemId(), item.getId()))
                .map(RAExtString::getValue)
                .collect(Collectors.toSet());
        assertEquals("Wrong values of assignment extension item " + item.getName(), new HashSet<>(Arrays.asList(expectedValues)), realValues);
    }

    protected void assertSearch(ItemName item, String value, int expectedCount, OperationResult result) throws SchemaException {
        ObjectQuery query = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_EXTENSION, item)
                .eq(value)
                .build();
        SearchResultList<PrismObject<UserType>> found = repositoryService
                .searchObjects(UserType.class, query, null, result);
        if (verbose) {
            displayValue("Found", found);
        }
        assertEquals("Wrong # of objects found", expectedCount, found.size());
    }

    protected void displayValue(String title, DebugDumpable value) {
        displayDumpable(title, value);
    }

    @Override
    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }

    protected <S, Q extends FlexibleRelationalPathBase<R>, R> List<R> select(
            QueryTableMapping<S, Q, R> mapping, Predicate... conditions) {
        try (JdbcSession jdbcSession = createJdbcSession().startReadOnlyTransaction()) {
            Q alias = mapping.defaultAlias();
            SQLQuery<R> query = jdbcSession.newQuery()
                    .select(alias)
                    .from(alias)
                    .where(conditions);
            PrimaryKey<R> primaryKey = alias.getPrimaryKey();
            if (primaryKey != null) {
                for (Path<?> pkColumn : primaryKey.getLocalColumns()) {
                    if (pkColumn instanceof ComparablePath) {
                        query.orderBy(((ComparablePath<?>) pkColumn).asc());
                    }
                }
            }

            return query.fetch();
        }
    }

    protected <S, Q extends FlexibleRelationalPathBase<R>, R> long count(
            QueryTableMapping<S, Q, R> mapping, Predicate... conditions) {
        try (JdbcSession jdbcSession = createJdbcSession().startReadOnlyTransaction()) {
            Q alias = mapping.defaultAlias();
            return jdbcSession.newQuery()
                    .select(alias)
                    .from(alias)
                    .where(conditions)
                    .fetchCount();
        }
    }

    /** Creates new {@link JdbcSession} based on {@link #baseHelper} setup. */
    protected JdbcSession createJdbcSession() {
        return new SqlRepoContext(baseHelper.getConfiguration(), baseHelper.dataSource(), schemaService, null)
                .newJdbcSession();
    }
}
