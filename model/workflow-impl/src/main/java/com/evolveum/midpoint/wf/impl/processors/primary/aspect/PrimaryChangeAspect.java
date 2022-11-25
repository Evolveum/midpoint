/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 * Change aspect deals with a given (elementary) kind of primary-stage change. Examples of change aspects:
 *  - AddRoleAssignmentAspect
 *  - CreateUserAspect
 *  - ChangeAttributeXAspect (X is an attribute of a user)
 *  - ...
 *
 * Change aspect plays a role on these occasions:
 * 1) When a change arrives - change aspect tries to recognize whether the change
 *    contains relevant delta(s); if so, it prepares instruction(s) to start related workflow approval process(es).
 * 2) When a process instance finishes, change aspect:
 *     - modifies the delta(s) related to particular process instance and passes them along, to be executed,
 *     - provides a list of approvers that is to be stored in modified object's metadata.
 * 3) When a user wants to work on his task, the change aspect prepares a form to be presented to the user.
 * 4) When a user asks about the state of process instance(s), the change aspect prepares that part of the
 *    answer that is specific to individual process.
 */
public interface PrimaryChangeAspect {

    /**
     * Examines the change and determines whether there are pieces that require (change type specific)
     * approval, for example, if there are roles added.
     *
     * If yes, it takes these deltas out of the original change and prepares instruction(s) to start wf process(es).
     *
     * @param objectTreeDeltas Change to be examined and modified by implementation of this method
     * @param ctx
     * @param result Operation result - the method should report any errors here (TODO what about creating subresults?)
     * @return list of start process instructions  @see WfTaskCreationInstruction
     */
    @NotNull
    <T extends ObjectType> List<PcpStartInstruction> getStartInstructions(
            @NotNull ObjectTreeDeltas<T> objectTreeDeltas,
            @NotNull ModelInvocationContext<T> ctx,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Returns true if this aspect is enabled by default, i.e. even if not listed in primary change processor configuration.
     */
    boolean isEnabledByDefault();

    boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfigurationType);

    String getBeanName();
}
