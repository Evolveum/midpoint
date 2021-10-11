/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A change processor can be viewed as a kind of framework supporting customer-specific
 * workflow code. Individual change processors are specialized in their areas, allowing
 * customer code to focus on business logic with minimal effort.
 *
 * The name "change processor" is derived from the fact that primary purpose of this
 * framework is to process change requests coming from the model.
 *
 * TODO find a better name
 *
 * However, a change processor has many more duties, e.g.
 *
 * (1) recognizes the instance (instances) of given kind of change within model operation context,
 * (2) processes the result of the workflow process instances when they are finished,
 * (3) presents (externalizes) the content of process instances to outside world: to the GUI, auditing, and notifications.
 *
 * Currently, there are the following change processors implemented or planned:
 * - PrimaryChangeProcessor: manages approvals of changes of objects (in model's primary stage)
 * - GeneralChangeProcessor: manages any change, as configured by the system engineer/administrator
 *
 * @author mederly
 */
public interface ChangeProcessor {

    /**
     * Processes workflow-related aspect of a model operation. Namely, tries to find whether user interaction is necessary,
     * and arranges everything to carry out that interaction.
     *
     * @param context Model context of the operation.
     * @param wfConfigurationType Current workflow configuration (part of the system configuration).
     * @param task Task in context of which the operation is carried out.
     * @param result Where to put information on operation execution.
     * @return non-null value if it processed the request;
     *              BACKGROUND = the process was "caught" by the processor, and continues in background,
     *              FOREGROUND = nothing was left on background, the model operation should continue in foreground,
     *              ERROR = something wrong has happened, there's no point in continuing with this operation.
     *         null if the request is not relevant to this processor
     *
     * Actually, the FOREGROUND return value is quite unusual, because the change processor cannot
     * know in advance whether other processors would not want to process the invocation from the model.
     */
    @Nullable
    HookOperationMode processModelInvocation(@NotNull ModelInvocationContext<?> ctx, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Handles an event from WfMS that indicates finishing of the workflow process instance.
     * Usually, at this point we see what was approved (and what was not) and continue with model operation(s).
     *
     * @param event
     * @param wfTask
     * @param result Here should be stored information about whether the finalization was successful or not
     * @throws SchemaException
     */
    void onProcessEnd(EngineInvocationContext ctx, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, PreconditionViolationException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException;

    /**
     * Prepares a process instance-related audit record.
     *
     * @param variables
     * @param aCase
     * @param stage
     * @param result
     * @return
     */
    AuditEventRecord prepareProcessInstanceAuditRecord(CaseType aCase, AuditEventStage stage, ApprovalContextType wfContext, OperationResult result);

    /**
     * Prepares a work item-related audit record.
     */
    // workItem contains taskRef, assignee, candidates resolved (if possible)
    AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem,
            CaseType aCase, OperationResult result);

    AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase, OperationResult result);

    MiscHelper getMiscHelper();

    PrismContext getPrismContext();

    RelationRegistry getRelationRegistry();
}

