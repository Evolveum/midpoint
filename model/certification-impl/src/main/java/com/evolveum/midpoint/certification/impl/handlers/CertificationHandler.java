/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.Collection;

public interface CertificationHandler {

    /**
     * Creates certification cases for a given midPoint object (e.g. user, role, org, ...).
     * It's expected to use campaign.scopeDefinition to do that.
     */
    <F extends AssignmentHolderType> Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<F> object,
            AccessCertificationCampaignType campaign, Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Implements the automated REVOKE for a given certification case.
     */
    void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task,
            OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Returns the default objectType to search for when preparing a list of certification cases.
     */
    QName getDefaultObjectType();
}
