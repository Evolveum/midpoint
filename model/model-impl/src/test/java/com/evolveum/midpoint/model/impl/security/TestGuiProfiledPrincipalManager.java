/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.DISABLED;

import java.util.List;
import java.util.function.Consumer;
import com.evolveum.midpoint.authentication.api.MidpointSessionRegistry;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.result.OperationResult;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGuiProfiledPrincipalManager extends AbstractInternalModelIntegrationTest {

    @Test
    public void test100DeputyNoLimits() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingAll(),
                a -> {});
    }

    @Test // MID-4111
    public void test110DeputyAssignmentDisabled() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.setActivation(new ActivationType().administrativeStatus(DISABLED)));
    }

    @Test // MID-4111
    public void test120DeputyAssignmentNotValid() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.setActivation(new ActivationType().validTo("2017-03-31T00:00:00+01:00")));
    }

    @Test
    public void test130DeputyAssignmentFalseCondition() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.beginCondition()
                        .beginExpression()
                        .expressionEvaluator(
                                new JAXBElement<>(new QName("script"),
                                        ScriptExpressionEvaluatorType.class,
                                        new ScriptExpressionEvaluatorType().code("false"))
                        ));
    }

    @Test
    public void test140DeputyBlockOtherPrivileges() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone(),
                (a) -> a.limitOtherPrivileges(new OtherPrivilegesLimitationType()));
    }

    @Test
    public void test150DeputyAllowOnlyCases() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone().allow(OtherPrivilegesLimitations.Type.CASES),
                (a) -> a.limitOtherPrivileges(
                        new OtherPrivilegesLimitationType()
                                .caseManagementWorkItems(new WorkItemSelectorType()
                                        .all(true))));
    }

    @Test
    public void test160DeputyAllowOnlyCasesLegacy() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone().allow(OtherPrivilegesLimitations.Type.CASES),
                (a) -> a.limitOtherPrivileges(
                        new OtherPrivilegesLimitationType()
                                .approvalWorkItems(new WorkItemSelectorType()
                                        .all(true))));
    }

    /**
     * Two sessions of the same user have two principals (equal by OID), each with its own compiled profile.
     * Change in system configuration has to invalidate both of them (issue 11041).
     */
    @Test
    public void test200InvalidateProfilesOfAllSessions() throws Exception {
        given("two sessions of the same user");
        OperationResult result = createOperationResult();
        GuiProfiledPrincipal olderPrincipal = focusProfileService.getPrincipal(userAdministrator, null, result);
        GuiProfiledPrincipal newerPrincipal = focusProfileService.getPrincipal(userAdministrator, null, result);
        assertThat(newerPrincipal).isNotSameAs(olderPrincipal).isEqualTo(olderPrincipal);

        TestSessionRegistry sessionRegistry = new TestSessionRegistry();
        sessionRegistry.registerNewSession("older-session", olderPrincipal);
        sessionRegistry.registerNewSession("newer-session", newerPrincipal);

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);
        try {
            when("system configuration is changed");
            manager.invalidate(SystemConfigurationType.class, null, false,null);
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", null);
        }

        then("profiles of both sessions are invalid");
        assertThat(olderPrincipal.getCompiledGuiProfile().isInvalid()).as("older session profile invalid").isTrue();
        assertThat(newerPrincipal.getCompiledGuiProfile().isInvalid()).as("newer session profile invalid").isTrue();
    }

    private static class TestSessionRegistry extends SessionRegistryImpl implements MidpointSessionRegistry {

        @Override
        public List<SessionInformation> getLoggedInUsersSession(Object principal) {
            return getAllSessions(principal, false);
        }
    }

    private void executeDeputyLimitationsTest(
            OtherPrivilegesLimitations.Limitation expectedJackLimitation, Consumer<AssignmentType> assignmentModifier)
            throws CommonException {

        given();
        AssignmentType assignment = new AssignmentType()
                .targetRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
        assignmentModifier.accept(assignment);

        UserType deputy = prismContext.createObjectable(UserType.class)
                .name("deputy")
                .oid("deputy")
                .assignment(assignment);

        when();
        AbstractIntegrationTest.display("Logging in as", deputy);
        login(deputy.asPrismObject());

        then();
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        OtherPrivilegesLimitations limitations = principal.getOtherPrivilegesLimitations();
        displayDumpable("other privileges limitations", limitations);
        var limitation = limitations.get(UserType.class, USER_JACK_OID);
        assertThat(limitation).as("limitation for jack").isEqualTo(expectedJackLimitation);
    }
}
