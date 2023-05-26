/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.DISABLED;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.AbstractIntegrationTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGuiProfiledPrincipalManager extends AbstractInternalModelIntegrationTest {

    @Test
    public void test100DeputyNoLimits() throws Exception {
        executeDeputyLimitationsTest(Collections.emptyList(), null);
    }

    @Test // MID-4111
    public void test110DeputyAssignmentDisabled() throws Exception {
        executeDeputyLimitationsTest(null,
                (a) -> a.setActivation(new ActivationType().administrativeStatus(DISABLED)));
    }

    @Test // MID-4111
    public void test120DeputyAssignmentNotValid() throws Exception {
        executeDeputyLimitationsTest(null,
                (a) -> a.setActivation(new ActivationType().validTo("2017-03-31T00:00:00+01:00")));
    }

    @Test
    public void test130DeputyAssignmentFalseCondition() throws Exception {
        executeDeputyLimitationsTest(null,
                (a) -> a.beginCondition()
                        .beginExpression()
                        .expressionEvaluator(
                                new JAXBElement<ScriptExpressionEvaluatorType>(new QName("script"),
                                        ScriptExpressionEvaluatorType.class,
                                        new ScriptExpressionEvaluatorType().code("false"))
                        ));
    }

    @Test
    public void test140DeputyBlockOtherPrivileges() throws Exception {
        executeDeputyLimitationsTest(Collections.singletonList(new OtherPrivilegesLimitationType()),
                (a) -> a.limitOtherPrivileges(new OtherPrivilegesLimitationType()));
    }

    private void executeDeputyLimitationsTest(
            List<OtherPrivilegesLimitationType> expectedLimitations,
            Consumer<AssignmentType> assignmentModifier)
            throws CommonException {

        // GIVEN
        AssignmentType assignment = new AssignmentType()
                .targetRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
        if (assignmentModifier != null) {
            assignmentModifier.accept(assignment);
        }

        UserType deputy = prismContext.createObjectable(UserType.class)
                .name("deputy")
                .oid("deputy")
                .assignment(assignment);

        // WHEN
        when();
        AbstractIntegrationTest.display("Logging in as", deputy);
        login(deputy.asPrismObject());

        // THEN
        then();
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        Collection<DelegatorWithOtherPrivilegesLimitations> delegators =
                principal.getDelegatorWithOtherPrivilegesLimitationsCollection();
        AbstractIntegrationTest.display("delegators with other privileges limitations", delegators);
        if (expectedLimitations == null) {
            AssertJUnit.assertEquals("Wrong # of delegator records: " + DebugUtil.debugDump(delegators), 0, delegators.size());
        } else {
            AssertJUnit.assertEquals("Wrong # of delegator records: " + DebugUtil.debugDump(delegators), 1, delegators.size());
            DelegatorWithOtherPrivilegesLimitations record = delegators.iterator().next();
            AssertJUnit.assertEquals("Unexpected limitations: " + DebugUtil.debugDump(delegators), new HashSet<>(expectedLimitations), new HashSet<>(record.getLimitations()));
        }
    }
}
