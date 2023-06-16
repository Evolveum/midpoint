/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine;

import com.evolveum.midpoint.cases.impl.engine.helpers.SimpleStageOpeningHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.cases.impl.engine.actions.ActionFactory;
import com.evolveum.midpoint.cases.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.cases.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.cases.impl.helpers.AuthorizationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseExpressionEvaluationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.cases.impl.helpers.NotificationHelper;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * Commonly useful beans e.g. for {@link CaseEngineOperationImpl}.
 */
@Component
public class CaseBeans {

    @Autowired public Clock clock;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired public PrismContext prismContext;
    @Autowired public SecurityEnforcer securityEnforcer;
    @Autowired public AuditHelper modelAuditHelper;
    @Autowired public NotificationHelper notificationHelper;
    @Autowired public CaseMiscHelper miscHelper;
    @Autowired public TriggerHelper triggerHelper;
    @Autowired public CaseExpressionEvaluationHelper expressionEvaluationHelper;
    @Autowired public WorkItemHelper workItemHelper;
    @Autowired public AuthorizationHelper authorizationHelper;
    @Autowired public ActionFactory actionFactory;
    @Autowired public CaseEventDispatcher caseEventDispatcher;
    @Autowired public TaskManager taskManager;
    @Autowired public SimpleStageOpeningHelper simpleStageOpeningHelper;
}
