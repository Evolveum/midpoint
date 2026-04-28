/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.mcp.api.McpPublicErrorMessages;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedFilterSpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditExplainRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpException;
import com.evolveum.midpoint.mcp.api.MidpointMcpObjectView;
import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchResult;
import com.evolveum.midpoint.mcp.api.MidpointMcpTypeSchemaView;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link MidpointMcpServiceImpl} using JDK proxies (no Mockito).
 */
public class MidpointMcpServiceImplTest extends AbstractUnitTest {

    private static final Task TASK = NullTaskImpl.INSTANCE;

    private CapturingModelHandler modelHandler;
    private CapturingAuditHandler auditHandler;
    private MidpointMcpServiceImpl service;

    @BeforeClass
    public void initPrismContextIfNeeded() throws com.evolveum.midpoint.util.exception.SchemaException, IOException, SAXException {
        if (PrismContext.get() == null) {
            PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        }
    }

    @BeforeMethod
    public void createService() {
        modelHandler = new CapturingModelHandler();
        auditHandler = new CapturingAuditHandler();
        ModelService modelService = (ModelService) Proxy.newProxyInstance(
                ModelService.class.getClassLoader(),
                new Class<?>[] { ModelService.class },
                modelHandler);
        ModelAuditService modelAuditService = (ModelAuditService) Proxy.newProxyInstance(
                ModelAuditService.class.getClassLoader(),
                new Class<?>[] { ModelAuditService.class },
                auditHandler);

        service = new MidpointMcpServiceImpl();
        inject(service, "modelService", modelService);
        inject(service, "modelAuditService", modelAuditService);
        inject(service, "orgStructFunctions", newNoOpOrgStructFunctions());
        inject(service, "prismContext", PrismContext.get());
        inject(service, "securityEnforcer", newAllowAllSecurityEnforcer());
    }

    @Test
    public void describeObjectTypeSchemaAuthorizesModelRead() {
        AtomicReference<String> lastOperationUrl = new AtomicReference<>();
        inject(service, "securityEnforcer", newCapturingAllowAllSecurityEnforcer(lastOperationUrl));

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        service.describeObjectTypeSchema("users", null, TASK, result);

        assertEquals(lastOperationUrl.get(), ModelAuthorizationAction.READ.getUrl());
    }

    @Test
    public void explainObjectAuthorizesGetObject() throws Exception {
        AtomicReference<String> lastOperationUrl = new AtomicReference<>();
        inject(service, "securityEnforcer", newCapturingAllowAllSecurityEnforcer(lastOperationUrl));

        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u1");
        user.asObjectable().setName(poly("a"));
        modelHandler.getObjectResult = user;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        service.explainObject("users", "u1", null, null, TASK, result);

        assertEquals(lastOperationUrl.get(), RestAuthorizationAction.GET_OBJECT.getUri());
    }

    @Test
    public void searchObjectsAuthorizesSearchObjects() throws Exception {
        AtomicReference<String> lastOperationUrl = new AtomicReference<>();
        inject(service, "securityEnforcer", newCapturingAllowAllSecurityEnforcer(lastOperationUrl));

        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u1");
        user.asObjectable().setName(poly("bob"));
        modelHandler.searchResult = new SearchResultList<>(List.of(user));
        modelHandler.count = 1;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        service.searchObjects(search("users", null, 10, 0, null), TASK, result);

        assertEquals(lastOperationUrl.get(), RestAuthorizationAction.SEARCH_OBJECTS.getUri());
    }

    @Test
    public void describeObjectTypeSchemaUsersReturnsAttributesAndExtension() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpTypeSchemaView view = service.describeObjectTypeSchema("users", null, TASK, result);

        assertEquals(view.getType(), "users");
        List<MidpointMcpSchemaAttribute> attrs = view.getAttributes();
        assertTrue(attrs != null && !attrs.isEmpty(), "schema attributes expected");
        assertTrue(attrs.stream().anyMatch(a -> "name".equals(a.getPath())), "expected top-level name path");
        assertNotNull(
                PrismContext.get()
                        .getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(UserType.class)
                        .findContainerDefinition(ObjectType.F_EXTENSION),
                "User object definition should include extension container (items appear when registered in schema)");
        assertTrue(
                attrs.stream().noneMatch(a -> a.getPath().contains("operationExecution")),
                "operationExecution subtree should be omitted");
        assertTrue(attrs.stream().noneMatch(a -> a.getPath().startsWith("metadata.")),
                "metadata subtree should be omitted from default schema");
        assertTrue(attrs.stream().noneMatch(a -> a.getPath().startsWith("lensContext.")),
                "lensContext subtree should be omitted");
        assertTrue(attrs.stream().noneMatch(a -> a.getPath().startsWith("trigger.")),
                "trigger subtree should be omitted");
        assertTrue(attrs.stream().noneMatch(a -> "jpegPhoto".equals(a.getPath())),
                "jpegPhoto should be omitted");
        assertTrue(attrs.stream().anyMatch(a -> "assignment.targetRef".equals(a.getPath())),
                "expected assignment.targetRef at default depth");
        Optional<MidpointMcpSchemaAttribute> effStatus = attrs.stream()
                .filter(a -> "activation.effectiveStatus".equals(a.getPath()))
                .findFirst();
        assertTrue(effStatus.isPresent(), "activation.effectiveStatus expected at depth 2");
        assertNotNull(effStatus.get().getEnum(), "enumeration values from Prism schema");
        assertTrue(
                effStatus.get().getEnum().contains("ENABLED")
                        && effStatus.get().getEnum().contains("DISABLED")
                        && effStatus.get().getEnum().contains("ARCHIVED"),
                "ActivationStatusType literals");
    }

    @Test
    public void describeObjectTypeSchemaMaxDepthTwoLimitsPathSegments() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpTypeSchemaView view = service.describeObjectTypeSchema("users", 2, TASK, result);

        for (MidpointMcpSchemaAttribute a : view.getAttributes()) {
            assertTrue(
                    pathSegmentCount(a.getPath()) <= 2,
                    "path exceeds maxDepth: " + a.getPath());
        }
    }

    @Test
    public void describeObjectTypeSchemaUnlimitedReturnsMoreThanDefaultDepth() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        int shallow = service.describeObjectTypeSchema("users", null, TASK, result).getAttributes().size();
        int deep = service.describeObjectTypeSchema("users", 0, TASK, result).getAttributes().size();
        assertTrue(deep >= shallow, "unlimited depth should not drop attributes");
        assertTrue(deep > shallow, "user schema should have paths deeper than default depth 2");
    }

    @Test
    public void describeObjectTypeSchemaRejectsUnknownType() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.describeObjectTypeSchema("unknown", null, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
        }
    }

    private static int pathSegmentCount(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '.') {
                n++;
            }
        }
        return n;
    }

    @Test
    public void explainUserSuccess() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u1");
        PolyStringType name = new PolyStringType();
        name.setOrig("alice");
        user.asObjectable().setName(name);
        modelHandler.getObjectResult = user;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("users", "u1", null, null, TASK, result);

        assertEquals(view.getValues().get("oid"), "u1");
        assertEquals(view.getValues().get("type"), "users");
        assertEquals(view.getValues().get("name"), "alice");
        assertNotNull(view.getValues().get("summary"));
        assertTrue(view.getValues().get("summary").toString().contains("User 'alice'"));
        assertNull(view.getValues().get("activation.administrativeStatus"));
    }

    @Test
    public void explainUserIncludesActivationWhenPresent() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u-act");
        user.asObjectable().setName(poly("act-user"));
        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        activation.setEffectiveStatus(ActivationStatusType.ENABLED);
        user.asObjectable().setActivation(activation);
        modelHandler.getObjectResult = user;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("users", "u-act", null, null, TASK, result);

        assertEquals(view.getValues().get("activation.administrativeStatus"), "ENABLED");
        assertEquals(view.getValues().get("activation.effectiveStatus"), "ENABLED");
    }

    @Test
    public void explainUserNotFound() {
        modelHandler.getObjectError = new ObjectNotFoundException("gone");

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.explainObject("users", "missing", null, null, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getCode(), "not_found");
            assertEquals(ex.getStatus(), 404);
            assertEquals(ex.getMessage(), McpPublicErrorMessages.NOT_FOUND);
        }
    }

    @Test
    public void explainUserForbidden() {
        modelHandler.getObjectError = new SecurityViolationException("nope");

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.explainObject("users", "u1", null, null, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getCode(), "forbidden");
            assertEquals(ex.getStatus(), 403);
            assertEquals(ex.getMessage(), McpPublicErrorMessages.ACCESS_DENIED);
        }
    }

    @Test
    public void explainObjectRejectsUnsupportedType() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.explainObject("reports", "x", null, null, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getCode(), "bad_request");
            assertEquals(ex.getStatus(), 400);
        }
    }

    @Test
    public void explainObjectNormalizesTypeCase() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u1");
        user.asObjectable().setName(poly("alice"));
        modelHandler.getObjectResult = user;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("Users", "u1", null, null, TASK, result);
        assertEquals(view.getValues().get("type"), "users");
    }

    @Test
    public void searchUsersRejectsNonPositiveLimit() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(search("users", null, 0, 0, null), TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getCode(), "bad_request");
            assertEquals(ex.getStatus(), 400);
        }
    }

    @Test
    public void searchUsersRejectsNegativeOffset() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(search("users", null, 10, -1, null), TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getCode(), "bad_request");
            assertEquals(ex.getStatus(), 400);
        }
    }

    @Test
    public void searchUsersCapsLimitAt100() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u1");
        PolyStringType name = new PolyStringType();
        name.setOrig("bob");
        user.asObjectable().setName(name);
        modelHandler.searchResult = new SearchResultList<>(List.of(user));
        modelHandler.count = 1;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult searchResult = service.searchObjects(search("users", null, 500, 0, null), TASK, result);

        assertEquals(searchResult.getLimit(), 100);
        assertNotNull(modelHandler.lastSearchQuery);
        assertNotNull(modelHandler.lastSearchQuery.getPaging());
        assertEquals(modelHandler.lastSearchQuery.getPaging().getMaxSize(), Integer.valueOf(100));
    }

    @Test
    public void searchUsersOmitsHeavyExplainFieldsByDefault() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u-heavy");
        user.asObjectable().setName(poly("heavy"));
        modelHandler.searchResult = new SearchResultList<>(List.of(user));
        modelHandler.count = 1;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult searchResult = service.searchObjects(search("users", null, 10, 0, null), TASK, result);

        assertNull(searchResult.getItems().getFirst().getValues().get("assignment"));
        assertNull(searchResult.getItems().getFirst().getValues().get("linkRef"));
        assertNull(searchResult.getItems().getFirst().getValues().get("parentOrgRef"));
    }

    @Test
    public void searchUsersHappyPath() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u2");
        PolyStringType name = new PolyStringType();
        name.setOrig("carol");
        user.asObjectable().setName(name);
        modelHandler.searchResult = new SearchResultList<>(List.of(user));
        modelHandler.count = 42;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult searchResult = service.searchObjects(search("users", "car", null, null, null), TASK, result);

        assertEquals(searchResult.getType(), "users");
        assertEquals(searchResult.getTotalCount(), 42);
        assertEquals(searchResult.getItems().size(), 1);
        assertEquals(searchResult.getItems().getFirst().getValues().get("oid"), "u2");
        assertEquals(searchResult.getLimit(), 20);
        assertEquals(searchResult.getOffset(), 0);
        assertEquals(searchResult.getUsedQueryMode(), "simple");
        assertNull(searchResult.getTranslatedMql());
        assertNull(searchResult.getItems().getFirst().getValues().get("activation.administrativeStatus"));
    }

    @Test
    public void searchMutuallyExclusiveModesRejected() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchRequest req = search("users", "a", null, null, null);
        req.setMql("name = \"x\"");
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getStatus(), 400);
            assertTrue(ex.getMessage().contains("Use only one"));
        }
    }

    @Test
    public void searchAdvancedQueryTranslatesMql() throws Exception {
        PrismObject<UserType> user = PrismContext.get().createObject(UserType.class);
        user.asObjectable().setOid("u3");
        user.asObjectable().setName(poly("jack"));
        modelHandler.searchResult = new SearchResultList<>(List.of(user));
        modelHandler.count = 1;

        MidpointMcpAdvancedQuerySpec aq = new MidpointMcpAdvancedQuerySpec();
        MidpointMcpAdvancedFilterSpec f1 = new MidpointMcpAdvancedFilterSpec();
        f1.setPath("givenName");
        f1.setOp("startsWith");
        f1.setValue("J");
        aq.getFilters().add(f1);
        MidpointMcpAdvancedFilterSpec f2 = new MidpointMcpAdvancedFilterSpec();
        f2.setPath("activation.effectiveStatus");
        f2.setOp("eq");
        f2.setValue("ENABLED");
        aq.getFilters().add(f2);

        MidpointMcpSearchRequest req = search("users", null, 10, 0, null);
        req.setAdvancedQuery(aq);

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult out = service.searchObjects(req, TASK, result);

        assertEquals(out.getUsedQueryMode(), "advancedQuery");
        assertEquals(
                out.getTranslatedMql(),
                "givenName startsWith \"J\" and activation/effectiveStatus = \"ENABLED\"");
        assertNotNull(modelHandler.lastSearchQuery);
    }

    @Test
    public void searchCasesAdvancedQueryStateEqOpen() throws Exception {
        PrismObject<CaseType> aCase = PrismContext.get().createObject(CaseType.class);
        aCase.asObjectable().setOid("case-oid-1");
        aCase.asObjectable().setName(poly("approval case"));
        modelHandler.searchResult = new SearchResultList<>(List.of(aCase));
        modelHandler.count = 1;

        MidpointMcpAdvancedQuerySpec aq = new MidpointMcpAdvancedQuerySpec();
        MidpointMcpAdvancedFilterSpec f = new MidpointMcpAdvancedFilterSpec();
        f.setPath("state");
        f.setOp("eq");
        f.setValue("open");
        aq.getFilters().add(f);

        MidpointMcpSearchRequest req = search("cases", null, 10, 0, null);
        req.setAdvancedQuery(aq);

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult out = service.searchObjects(req, TASK, result);

        assertEquals(out.getUsedQueryMode(), "advancedQuery");
        assertEquals(out.getTranslatedMql(), "state = \"open\"");
    }

    @Test
    public void explainCaseIncludesExplanationAndChildSearch() throws Exception {
        PrismObject<CaseType> aCase = PrismContext.get().createObject(CaseType.class);
        aCase.asObjectable().setOid("case-root");
        aCase.asObjectable().setName(poly("Root case"));
        aCase.asObjectable().setState("open");
        modelHandler.getObjectResult = aCase;
        modelHandler.searchResult = SearchResultList.empty();
        modelHandler.count = 0;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("cases", "case-root", null, null, TASK, result);

        assertEquals(view.getValues().get("type"), "cases");
        assertNotNull(view.getValues().get("explanation"));
        assertNotNull(view.getValues().get("childCases"));
        assertNotNull(view.getValues().get("currentStep"));
    }

    @Test
    public void searchAdvancedQueryUnknownPathRejected() {
        MidpointMcpAdvancedQuerySpec aq = new MidpointMcpAdvancedQuerySpec();
        MidpointMcpAdvancedFilterSpec f = new MidpointMcpAdvancedFilterSpec();
        f.setPath("totallyUnknownFieldXyz");
        f.setOp("exists");
        aq.getFilters().add(f);
        MidpointMcpSearchRequest req = search("users", null, 10, 0, null);
        req.setAdvancedQuery(aq);
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getStatus(), 400);
            assertTrue(ex.getMessage().contains("Unknown path"));
        }
    }

    @Test
    public void searchRawMqlParseErrorIsBadRequest() {
        MidpointMcpSearchRequest req = search("users", null, 10, 0, null);
        req.setMql("this is not valid mql !!!");
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException ex) {
            assertEquals(ex.getStatus(), 400);
            assertEquals(ex.getCode(), "bad_request");
            assertEquals(ex.getMessage(), "MQL could not be parsed.");
        }
    }

    @Test
    public void explainResourceActivationIsNull() throws Exception {
        PrismObject<ResourceType> resource = PrismContext.get().createObject(ResourceType.class);
        resource.asObjectable().setOid("r1");
        resource.asObjectable().setName(poly("res-a"));
        modelHandler.getObjectResult = resource;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("resources", "r1", null, null, TASK, result);

        assertEquals(view.getValues().get("type"), "resources");
        assertNull(view.getValues().get("activation.administrativeStatus"));
        assertTrue(view.getValues().get("summary").toString().contains("Resource 'res-a'"));
    }

    @Test
    public void explainShadowIncludesActivationWhenPresent() throws Exception {
        PrismObject<ShadowType> shadow = PrismContext.get().createObject(ShadowType.class);
        shadow.asObjectable().setOid("s1");
        shadow.asObjectable().setName(poly("acc-a"));
        ActivationType activation = new ActivationType();
        activation.setEffectiveStatus(ActivationStatusType.DISABLED);
        shadow.asObjectable().setActivation(activation);
        modelHandler.getObjectResult = shadow;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("shadows", "s1", null, null, TASK, result);

        assertEquals(view.getValues().get("type"), "shadows");
        assertEquals(view.getValues().get("activation.effectiveStatus"), "DISABLED");
        assertEquals(view.getSource(), "repository");
        assertEquals(view.getFetched(), Boolean.FALSE);
        assertEquals(modelHandler.getObjectOptionsCalls.size(), 1);
        assertTrue(isNoFetchOptions(modelHandler.getObjectOptionsCalls.get(0)));
    }

    @Test
    public void explainShadowFetchTrueUsesLiveOptionsThenNoFetchForCompare() throws Exception {
        PrismObject<ShadowType> shadow = PrismContext.get().createObject(ShadowType.class);
        shadow.asObjectable().setOid("s1");
        shadow.asObjectable().setName(poly("acc-a"));
        modelHandler.getObjectResult = shadow;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        service.explainObject("shadows", "s1", null, true, TASK, result);

        assertEquals(modelHandler.getObjectOptionsCalls.size(), 2);
        assertFalse(isNoFetchOptions(modelHandler.getObjectOptionsCalls.get(0)));
        assertTrue(isNoFetchOptions(modelHandler.getObjectOptionsCalls.get(1)));
    }

    @Test
    public void explainFetchRejectedForNonShadow() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.explainObject("users", "u1", null, true, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
            assertTrue(e.getMessage().contains("fetch"));
        }
    }

    @Test
    public void searchShadowRepositoryUsesNoFetch() throws Exception {
        PrismObject<ShadowType> shadow = PrismContext.get().createObject(ShadowType.class);
        shadow.asObjectable().setOid("s2");
        shadow.asObjectable().setName(poly("sh-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(shadow));
        modelHandler.count = 1;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchRequest shadowReq = search("shadows", null, 10, 0, null);
        shadowReq.setResourceOid("res-shadow-test");
        shadowReq.setShadowKind("account");
        MidpointMcpSearchResult r = service.searchObjects(shadowReq, TASK, result);
        assertEquals(r.getSource(), "repository");
        assertEquals(r.getFetched(), Boolean.FALSE);
        assertEquals(r.getSearchMode(), "repository");
        assertTrue(isNoFetchOptions(modelHandler.lastSearchOptions));
    }

    @Test
    public void searchResourceModeRequiresResourceOid() {
        MidpointMcpSearchRequest req = search("shadows", null, 10, 0, null);
        req.setSearchMode("resource");
        req.setShadowKind("account");
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
            assertTrue(e.getMessage().contains("resourceOid"));
        }
    }

    @Test
    public void searchFetchRejectedForUsers() {
        MidpointMcpSearchRequest req = search("users", null, 10, 0, null);
        req.setFetch(true);
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
        }
    }

    @Test
    public void searchShadowMqlResourceRefOnlyRejectedWithNotTypeScopedCode() {
        MidpointMcpSearchRequest req = search("shadows", null, 10, 0, null);
        req.setMql("resourceRef matches (oid = \"11111111-1111-1111-1111-111111111111\")");
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
            assertEquals(e.getCode(), "shadow_query_not_type_scoped");
            assertNotNull(e.getHint());
        }
    }

    @Test
    public void searchRepositoryResourceOidWithoutKindOrExpandRejected() {
        MidpointMcpSearchRequest req = search("shadows", null, 10, 0, null);
        req.setResourceOid("res-oid");
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.searchObjects(req, TASK, result);
            fail("expected MidpointMcpException");
        } catch (MidpointMcpException e) {
            assertEquals(e.getStatus(), 400);
            assertEquals(e.getCode(), "shadow_repository_scope_incomplete");
            assertNotNull(e.getHint());
        }
    }

    @Test
    public void explainArchetypeSuccess() throws Exception {
        PrismObject<ArchetypeType> archetype = PrismContext.get().createObject(ArchetypeType.class);
        archetype.asObjectable().setOid("a1");
        archetype.asObjectable().setName(poly("arch-a"));
        modelHandler.getObjectResult = archetype;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("archetypes", "a1", null, null, TASK, result);

        assertEquals(ArchetypeType.class, modelHandler.lastGetObjectClass);
        assertEquals(view.getValues().get("type"), "archetypes");
        assertTrue(view.getValues().get("summary").toString().contains("Archetype 'arch-a'"));
    }

    @Test
    public void searchArchetypesHappyPath() throws Exception {
        PrismObject<ArchetypeType> archetype = PrismContext.get().createObject(ArchetypeType.class);
        archetype.asObjectable().setOid("a2");
        archetype.asObjectable().setName(poly("arch-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(archetype));
        modelHandler.count = 1;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpSearchResult searchResult = service.searchObjects(search("archetypes", null, 10, 0, null), TASK, result);

        assertEquals(searchResult.getType(), "archetypes");
        assertEquals(searchResult.getItems().getFirst().getValues().get("oid"), "a2");
    }

    @Test
    public void explainConnectorAndSearchHappyPath() throws Exception {
        PrismObject<ConnectorType> connector = PrismContext.get().createObject(ConnectorType.class);
        connector.asObjectable().setOid("c1");
        connector.asObjectable().setName(poly("conn-a"));
        connector.asObjectable().setConnectorType("com.example.Connector");
        modelHandler.getObjectResult = connector;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView view = service.explainObject("connectors", "c1", null, null, TASK, result);
        assertEquals(view.getValues().get("type"), "connectors");
        assertNull(view.getValues().get("activation.administrativeStatus"));

        modelHandler.searchResult = new SearchResultList<>(List.of(connector));
        modelHandler.count = 1;
        MidpointMcpSearchResult search = service.searchObjects(search("connectors", null, 5, 0, null), TASK, result);
        assertEquals(search.getType(), "connectors");
        assertNull(search.getItems().getFirst().getValues().get("activation.administrativeStatus"));
    }

    @Test
    public void explainServiceAndSearchHappyPath() throws Exception {
        PrismObject<ServiceType> svc = PrismContext.get().createObject(ServiceType.class);
        svc.asObjectable().setOid("svc1");
        svc.asObjectable().setName(poly("svc-a"));
        modelHandler.getObjectResult = svc;

        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        assertEquals(service.explainObject("services", "svc1", null, null, TASK, result).getValues().get("type"), "services");

        modelHandler.searchResult = new SearchResultList<>(List.of(svc));
        modelHandler.count = 3;
        assertEquals(service.searchObjects(search("services", null, 10, 0, null), TASK, result).getTotalCount(), 3);
    }

    @Test
    public void explainTaskAndNodeHappyPath() throws Exception {
        PrismObject<TaskType> taskObj = PrismContext.get().createObject(TaskType.class);
        taskObj.asObjectable().setOid("t1");
        taskObj.asObjectable().setName(poly("task-a"));
        modelHandler.getObjectResult = taskObj;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpObjectView vTask = service.explainObject("tasks", "t1", null, null, TASK, result);
        assertEquals(vTask.getValues().get("type"), "tasks");
        assertNull(vTask.getValues().get("activation.administrativeStatus"));

        PrismObject<NodeType> node = PrismContext.get().createObject(NodeType.class);
        node.asObjectable().setOid("n1");
        node.asObjectable().setName(poly("node-a"));
        modelHandler.getObjectResult = node;
        MidpointMcpObjectView vNode = service.explainObject("nodes", "n1", null, null, TASK, result);
        assertEquals(vNode.getValues().get("type"), "nodes");
        assertNull(vNode.getValues().get("activation.administrativeStatus"));
    }

    @Test
    public void searchResourcesAndShadowsHappyPath() throws Exception {
        PrismObject<ResourceType> resource = PrismContext.get().createObject(ResourceType.class);
        resource.asObjectable().setOid("r2");
        resource.asObjectable().setName(poly("res-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(resource));
        modelHandler.count = 1;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        assertEquals(service.searchObjects(search("resources", null, 10, 0, null), TASK, result).getType(), "resources");

        PrismObject<ShadowType> shadow = PrismContext.get().createObject(ShadowType.class);
        shadow.asObjectable().setOid("s2");
        shadow.asObjectable().setName(poly("sh-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(shadow));
        modelHandler.count = 1;
        MidpointMcpSearchRequest shadowReq = search("shadows", null, 10, 0, null);
        shadowReq.setResourceOid("r2");
        shadowReq.setShadowKind("account");
        assertEquals(service.searchObjects(shadowReq, TASK, result).getType(), "shadows");
    }

    @Test
    public void searchTasksAndNodesHappyPath() throws Exception {
        PrismObject<TaskType> taskObj = PrismContext.get().createObject(TaskType.class);
        taskObj.asObjectable().setOid("t2");
        taskObj.asObjectable().setName(poly("task-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(taskObj));
        modelHandler.count = 5;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        assertEquals(service.searchObjects(search("tasks", null, 10, 0, null), TASK, result).getTotalCount(), 5);

        PrismObject<NodeType> node = PrismContext.get().createObject(NodeType.class);
        node.asObjectable().setOid("n2");
        node.asObjectable().setName(poly("node-b"));
        modelHandler.searchResult = new SearchResultList<>(List.of(node));
        assertEquals(service.searchObjects(search("nodes", null, 10, 0, null), TASK, result).getType(), "nodes");
    }

    @Test
    public void searchAuditRejectsQueryAndAdvancedTogether() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpAuditSearchRequest req = new MidpointMcpAuditSearchRequest();
        req.setQuery("admin");
        req.setAdvancedQuery(new MidpointMcpAdvancedQuerySpec());
        try {
            service.searchAudit(req, TASK, result);
            fail("expected bad_request");
        } catch (MidpointMcpException e) {
            assertEquals(400, e.getStatus());
        }
    }

    @Test
    public void searchAuditDefaultPagingAndCapturesQuery() {
        auditHandler.searchList = new SearchResultList<>();
        auditHandler.count = 0;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpAuditSearchRequest req = new MidpointMcpAuditSearchRequest();
        service.searchAudit(req, TASK, result);
        assertNotNull(auditHandler.lastSearchQuery);
        ObjectPaging p = auditHandler.lastSearchQuery.getPaging();
        assertNotNull(p);
        assertEquals(Integer.valueOf(100), p.getMaxSize());
        assertEquals(0, p.getOffset());
        assertNotNull(auditHandler.lastCountQuery);
    }

    @Test
    public void searchAuditSimpleUuidQueryDoesNotUseTargetNamePaths() {
        auditHandler.searchList = new SearchResultList<>();
        auditHandler.count = 0;
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        MidpointMcpAuditSearchRequest req = new MidpointMcpAuditSearchRequest();
        req.setQuery("40cb9035-8990-4516-9e6c-1c290da7f6d8");

        service.searchAudit(req, TASK, result);

        assertNotNull(auditHandler.lastSearchQuery);
        String q = String.valueOf(auditHandler.lastSearchQuery.getFilter());
        assertFalse(q.contains("targetName"), "simple UUID query should not traverse initiator/target targetName");
        assertTrue(q.contains("targetRef"), "simple UUID query should still allow target OID matching");
    }

    @Test
    public void explainAuditRecordRequiresId() {
        OperationResult result = new OperationResult(MidpointMcpServiceImplTest.class.getName());
        try {
            service.explainAuditRecord(new MidpointMcpAuditExplainRequest(), TASK, result);
            fail("expected bad_request");
        } catch (MidpointMcpException e) {
            assertEquals(400, e.getStatus());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean isNoFetchOptions(Collection<?> options) {
        if (options == null) {
            return false;
        }
        return GetOperationOptions.isNoFetch((Collection<SelectorOptions<GetOperationOptions>>) (Collection) options);
    }

    private static MidpointMcpSearchRequest search(
            String type, String query, Integer limit, Integer offset, List<String> returnAttributes) {
        MidpointMcpSearchRequest r = new MidpointMcpSearchRequest();
        r.setType(type);
        r.setQuery(query);
        r.setLimit(limit);
        r.setOffset(offset);
        r.setReturnAttributes(returnAttributes);
        return r;
    }

    private static PolyStringType poly(String orig) {
        PolyStringType p = new PolyStringType();
        p.setOrig(orig);
        return p;
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            Field f = MidpointMcpServiceImpl.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static OrgStructFunctions newNoOpOrgStructFunctions() {
        return (OrgStructFunctions) Proxy.newProxyInstance(
                OrgStructFunctions.class.getClassLoader(),
                new Class<?>[] { OrgStructFunctions.class },
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (Collection.class.isAssignableFrom(rt)) {
                        return List.of();
                    }
                    return null;
                });
    }

    private static SecurityEnforcer newAllowAllSecurityEnforcer() {
        return newCapturingAllowAllSecurityEnforcer(new AtomicReference<>());
    }

    /**
     * Records the REST/model operation URL from the 7-arg {@code decideAccess} call (used by {@code authorize}).
     */
    private static SecurityEnforcer newCapturingAllowAllSecurityEnforcer(AtomicReference<String> lastOperationUrl) {
        return (SecurityEnforcer) Proxy.newProxyInstance(
                SecurityEnforcer.class.getClassLoader(),
                new Class<?>[] { SecurityEnforcer.class },
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return switch (method.getName()) {
                        case "decideAccess" -> {
                            if (method.getParameterCount() == 7) {
                                if (args[1] instanceof String url) {
                                    lastOperationUrl.set(url);
                                }
                                yield AccessDecision.ALLOW;
                            }
                            throw new UnsupportedOperationException(method.toString());
                        }
                        case "getMidPointPrincipal" -> null;
                        case "failAuthorization" -> throw new SecurityViolationException("Not authorized");
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                });
    }

    private static final class CapturingAuditHandler implements InvocationHandler {

        ObjectQuery lastSearchQuery;
        ObjectQuery lastCountQuery;
        SearchResultList<AuditEventRecordType> searchList = new SearchResultList<>();
        int count;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "supportsRetrieval" -> true;
                case "searchObjects" -> {
                    lastSearchQuery = (ObjectQuery) args[0];
                    yield searchList != null ? searchList : new SearchResultList<>();
                }
                case "countObjects" -> {
                    lastCountQuery = (ObjectQuery) args[0];
                    yield count;
                }
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }
    }

    private static final class CapturingModelHandler implements InvocationHandler {

        private Throwable getObjectError;
        private PrismObject<?> getObjectResult;
        private SearchResultList<PrismObject<?>> searchResult = SearchResultList.empty();
        private int count;
        private ObjectQuery lastSearchQuery;
        private Class<?> lastGetObjectClass;
        private Collection<?> lastSearchOptions;
        private Collection<?> lastCountOptions;
        private final List<Collection<?>> getObjectOptionsCalls = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "getObject" -> {
                    if (args != null && args.length > 0 && args[0] instanceof Class<?> c) {
                        lastGetObjectClass = c;
                    }
                    if (args != null && args.length > 2 && args[2] instanceof Collection<?> opts) {
                        getObjectOptionsCalls.add(opts);
                    }
                    if (getObjectError != null) {
                        throw getObjectError;
                    }
                    yield getObjectResult;
                }
                case "searchObjects" -> {
                    lastSearchQuery = (ObjectQuery) args[1];
                    lastSearchOptions = args.length > 2 && args[2] instanceof Collection<?> c ? c : null;
                    yield searchResult;
                }
                case "countObjects" -> {
                    lastCountOptions = args.length > 2 && args[2] instanceof Collection<?> c ? c : null;
                    yield count;
                }
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }
    }
}
