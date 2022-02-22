/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A change processor can be viewed as a kind of framework supporting customer-specific
 * approvals code. Individual change processors are specialized in their areas, allowing
 * customer code to focus on business logic with minimal effort.
 *
 * The name "change processor" is derived from the fact that main purpose of this
 * framework is to process change requests coming from the model.
 *
 * However, a change processor has many more duties, e.g.
 *
 * 1. recognizes the instance (instances) of given kind of change within model operation context,
 * 2. processes the result of the approval case objects when they are finished,
 * 3. presents (externalizes) the content of approval cases to outside world: to the GUI, auditing, and notifications.
 *
 * Currently, there is only a single change processor implemented. It is {@link PrimaryChangeProcessor} that manages
 * approvals of changes of objects that are captured during `primary` model state processing. They may deal with focus
 * or with a projection, but they must be entered by the user - i.e. _not_ determined by the projector.
 *
 * NOTE: Because the {@link PrimaryChangeProcessor} is the only one that is currently available, the code is not very
 * clean in this respect. Some parts of existing code assume that this is the only processor, while others are more
 * universal. Because we simply do not know for sure, we - for now - leave the code in this state: we neither do not
 * generalize it to multiple processors, nor we simplify it to concentrate on this single processor.
 */
public interface ChangeProcessor {

    /**
     * Processes approval-related aspect of a model operation.
     *
     * Namely, it tries to find whether user interaction is necessary, and arranges everything to carry out that interaction.
     *
     * @param ctx All information about the model operation, including e.g. model context.
     * @param result Where to put information on operation execution.
     * @return non-null value if it processed the request;
     *   {@link HookOperationMode#BACKGROUND} = the process was "caught" by the processor, and continues in background,
     *   {@link HookOperationMode#FOREGROUND} = nothing was left on background, the model operation should continue in foreground,
     *   {@link HookOperationMode#ERROR} = something wrong has happened, there's no point in continuing with this operation,
     *   `null` if the request is not relevant to this processor.
     *
     * Actually, the FOREGROUND return value is quite unusual, because the change processor cannot
     * know in advance whether other processors would not want to process the invocation from the model.
     */
    @Nullable
    HookOperationMode processModelInvocation(@NotNull ModelInvocationContext<?> ctx, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Handles the result of case processing. The case manager calls us when it finished its work on an approval case.
     * (With the state of {@link SchemaConstants#CASE_STATE_CLOSING}.) At this point we see what was approved
     * (and what was not) and we may start the real execution - or wait until all approval cases are resolved.
     *
     * Note that this method is called OUTSIDE the workflow engine computation - i.e. changes
     * are already committed into repository.
     */
    void finishCaseClosing(CaseEngineOperation operation, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, PreconditionViolationException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException;

    /**
     * Adds approval-specific information to the case-level audit record.
     */
    void enrichCaseAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation);

    /**
     * Adds approval-specific information to the work-item-level audit record.
     * TODO consider merging with {@link #enrichWorkItemDeletedAuditRecord(AuditEventRecord, CaseEngineOperation)}.
     */
    void enrichWorkItemCreatedAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation);

    /**
     * Adds approval-specific information to the work-item-level audit record.
     */
    void enrichWorkItemDeletedAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation);

    // TODO consider removing
    MiscHelper getMiscHelper();
}
