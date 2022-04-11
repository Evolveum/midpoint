/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.wf.impl.execution.ExecutionHelper;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpGeneralHelper;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Useful Spring beans for workflow-impl module.
 */
@Component
public class ApprovalBeans {

    @Autowired public Clock clock;
    @Autowired public ConfigurationHelper configurationHelper;
    @Autowired public ModelHelper modelHelper;
    @Autowired public StageComputeHelper stageComputeHelper;
    @Autowired public PcpGeneralHelper generalHelper;
    @Autowired public MiscHelper miscHelper;
    @Autowired public CaseMiscHelper caseMiscHelper;
    @Autowired public ExecutionHelper executionHelper;
    @Autowired public ExpressionEvaluationHelper expressionEvaluationHelper;

    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired public CaseEngineImpl caseEngine;

}
