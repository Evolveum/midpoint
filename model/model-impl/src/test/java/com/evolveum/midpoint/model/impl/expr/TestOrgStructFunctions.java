/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests {@link OrgStructFunctions} for parent orgs that are not persistent, e.g. orgs created on demand
 * in a simulation. A reference to such an org has no OID, only the type and the name.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestOrgStructFunctions extends AbstractInternalModelIntegrationTest {

    @Autowired private OrgStructFunctions orgStructFunctions;

    private static ObjectReferenceType parentOrgRefWithoutOid(String name) {
        return new ObjectReferenceType()
                .type(OrgType.COMPLEX_TYPE)
                .targetName(name);
    }

    private Collection<OrgType> getParentOrgs(UserType user, boolean preAuthorized, Task task, OperationResult result)
            throws Exception {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ModelExpressionEnvironment<>(task, result));
        try {
            return orgStructFunctions.getParentOrgs(user, preAuthorized);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    /** The parent org is not persistent, so it cannot be found. It is skipped; the call must not fail. */
    @Test
    public void test100ParentOrgWithoutOidPreAuthorized() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        UserType user = new UserType()
                .name("user-with-on-demand-org")
                .parentOrgRef(parentOrgRefWithoutOid("on-demand-org"));

        Collection<OrgType> parentOrgs = getParentOrgs(user, true, task, result);

        assertThat(parentOrgs).isEmpty();
    }

    /** The same, but the objects are read through the model, so the authorizations are applied. */
    @Test
    public void test110ParentOrgWithoutOid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        UserType user = new UserType()
                .name("user-with-on-demand-org")
                .parentOrgRef(parentOrgRefWithoutOid("on-demand-org"));

        Collection<OrgType> parentOrgs = getParentOrgs(user, false, task, result);

        assertThat(parentOrgs).isEmpty();
    }

    /** A persistent parent org is still returned when another parent org has no OID. */
    @Test
    public void test120PersistentAndNonPersistentParentOrgs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<OrgType> persistentOrg = new OrgType().name("persistent-parent-org").asPrismObject();
        String persistentOrgOid = repositoryService.addObject(persistentOrg, null, result);
        UserType user = new UserType()
                .name("user-with-both-orgs")
                .parentOrgRef(parentOrgRefWithoutOid("on-demand-org"))
                .parentOrgRef(persistentOrgOid, OrgType.COMPLEX_TYPE);

        Collection<OrgType> parentOrgs = getParentOrgs(user, true, task, result);

        assertThat(parentOrgs).extracting(OrgType::getOid).containsExactly(persistentOrgOid);
    }
    /**
     * Outside a simulation, a reference to a missing org is a dangling reference. The embedded copy of the org
     * in the reference must not be trusted: it would also bypass the authorizations.
     */
    @Test
    public void test130DanglingParentOrgWithEmbeddedObjectIsIgnoredOutsideSimulation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectReferenceType ref = new ObjectReferenceType()
                .oid(UUID.randomUUID().toString())
                .type(OrgType.COMPLEX_TYPE);
        ref.asReferenceValue().setObject(new OrgType().name("embedded-org").asPrismObject());
        UserType user = new UserType().name("user-with-dangling-org").parentOrgRef(ref);

        Collection<OrgType> parentOrgs = getParentOrgs(user, false, task, result);

        assertThat(parentOrgs).isEmpty();
    }

    /** The same for a reference that has no OID at all, only the embedded object. */
    @Test
    public void test140ParentOrgWithoutOidWithEmbeddedObjectIsIgnoredOutsideSimulation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectReferenceType ref = parentOrgRefWithoutOid("embedded-org");
        ref.asReferenceValue().setObject(new OrgType().name("embedded-org").asPrismObject());
        UserType user = new UserType().name("user-with-embedded-org").parentOrgRef(ref);

        Collection<OrgType> parentOrgs = getParentOrgs(user, true, task, result);

        assertThat(parentOrgs).isEmpty();
    }
}
