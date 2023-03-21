/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import java.util.Date;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.StartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * {@link StartInstruction} for primary change processor.
 *
 * Invariant: case.approvalContext != null
 */
public class PcpStartInstruction extends StartInstruction {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(PcpStartInstruction.class);

    private boolean isObjectCreationInstruction;

    private PcpStartInstruction(@NotNull ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
        super(changeProcessor, archetypeOid);
        aCase.setApprovalContext(new ApprovalContextType());
    }

    public static PcpStartInstruction createItemApprovalInstruction(ChangeProcessor changeProcessor,
            @NotNull ApprovalSchemaType approvalSchemaType, SchemaAttachedPolicyRulesType attachedPolicyRules) {
        PcpStartInstruction instruction = new PcpStartInstruction(changeProcessor,
                SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
        instruction.getApprovalContext().setApprovalSchema(approvalSchemaType);
        instruction.getApprovalContext().setPolicyRules(attachedPolicyRules);
        return instruction;
    }

    static PcpStartInstruction createEmpty(ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
        return new PcpStartInstruction(changeProcessor, archetypeOid);
    }

    public boolean isExecuteApprovedChangeImmediately() {
        ApprovalContextType aCtx = aCase.getApprovalContext();
        return aCtx != null && Boolean.TRUE.equals(aCtx.isImmediateExecution());
    }

    void setExecuteApprovedChangeImmediately(ModelContext<?> modelContext) {
        aCase.getApprovalContext().setImmediateExecution(
                ModelExecuteOptions.isExecuteImmediatelyAfterApproval(modelContext.getOptions()));
    }

    public void prepareCommonAttributes(
            PrimaryChangeAspect aspect, ModelContext<?> modelContext, PrismObject<? extends FocusType> requester) {
        if (requester != null) {
            setRequesterRef(requester);
        }

        setExecuteApprovedChangeImmediately(modelContext);

        getApprovalContext().setChangeAspect(aspect.getClass().getName());

        CaseCreationEventType event = new CaseCreationEventType();
        event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        if (requester != null) {
            event.setInitiatorRef(ObjectTypeUtil.createObjectRef(requester));
            // attorney does not need to be set here (for now)
        }
        event.setBusinessContext(((LensContext<?>) modelContext).getRequestBusinessContext());
        aCase.getEvent().add(event);
    }

    public void setDeltasToApprove(ObjectDelta<? extends ObjectType> delta) throws SchemaException {
        setDeltasToApprove(new ObjectTreeDeltas<>(delta));
    }

    public void setDeltasToApprove(ObjectTreeDeltas<?> objectTreeDeltas) throws SchemaException {
        isObjectCreationInstruction = isObjectCreationInstruction(objectTreeDeltas);
        getApprovalContext().setDeltasToApprove(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
    }

    private boolean isObjectCreationInstruction(ObjectTreeDeltas<?> deltasToApprove) {
        return deltasToApprove != null && deltasToApprove.getFocusChange() != null && deltasToApprove.getFocusChange().isAdd();
    }

    void setResultingDeltas(ObjectTreeDeltas<?> objectTreeDeltas) throws SchemaException {
        getApprovalContext().setResultingDeltas(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PrimaryChangeProcessor StartInstruction: (execute approved change immediately = ")
                .append(isExecuteApprovedChangeImmediately())
                .append(")\n");
        sb.append(super.debugDump(indent+1));
        return sb.toString();
    }

    boolean isObjectCreationInstruction() {
        return isObjectCreationInstruction;
    }
}
