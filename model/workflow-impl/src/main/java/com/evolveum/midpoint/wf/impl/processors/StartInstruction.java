/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

/**
 * A generic instruction to create a case (operation request or approval case).
 * May be subclassed in order to add further functionality.
 */
public class StartInstruction implements DebugDumpable {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(StartInstruction.class);

    protected final CaseType aCase;

    private final ChangeProcessor changeProcessor;

    //region Constructors
    protected StartInstruction(@NotNull ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
        this.changeProcessor = changeProcessor;
        aCase = new CaseType();
        ObjectReferenceType approvalArchetypeRef = ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE);
        aCase.getArchetypeRef().add(approvalArchetypeRef.clone());
        aCase.beginAssignment().targetRef(approvalArchetypeRef).end();
        aCase.setMetadata(new MetadataType());
        aCase.getMetadata().setCreateTimestamp(createXMLGregorianCalendar(new Date()));
    }

    public static StartInstruction create(ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
        return new StartInstruction(changeProcessor, archetypeOid);
    }
    //endregion

    // region Getters and setters
    private ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    public void setName(String name) {
        aCase.setName(PolyStringType.fromOrig(name));
    }

    public void setName(String name, LocalizableMessage localizable) {
        PolyStringType polyName = PolyStringType.fromOrig(name);
        if (localizable != null) {
            if (!(localizable instanceof SingleLocalizableMessage)) {
                throw new UnsupportedOperationException(
                        "Localizable messages other than SingleLocalizableMessage cannot be used for approval case names: "
                                + localizable);
            } else {
                polyName.setTranslation(PolyStringTranslationType.fromLocalizableMessage((SingleLocalizableMessage) localizable));
            }
        }
        aCase.setName(polyName);
    }

    public boolean startsWorkflowProcess() {
        ApprovalContextType actx = getApprovalContext();
        return actx != null && actx.getApprovalSchema() != null;
    }

    public void setModelContext(ModelContext<?> context) throws SchemaException {
        LensContextType bean;
        if (context != null) {
            boolean reduced = context.getState() == ModelState.PRIMARY;
            bean = ((LensContext<?>) context)
                    .toBean(reduced ? LensContext.ExportType.REDUCED : LensContext.ExportType.OPERATIONAL);
        } else {
            bean = null;
        }
        aCase.setModelContext(bean);
    }

    public void setObjectRef(ObjectReferenceType ref, OperationResult result) {
        ref = getChangeProcessor().getMiscHelper().resolveObjectReferenceName(ref, result);
        aCase.setObjectRef(ref);
    }

    public void setObjectRef(ModelInvocationContext<?> ctx) {
        ObjectType focus = ctx.getFocusObjectNewOrOld();
        ObjectDelta<?> primaryDelta = ctx.modelContext.getFocusContext().getPrimaryDelta();
        ObjectReferenceType ref;
        if (primaryDelta != null && primaryDelta.isAdd()) {
            ref = ObjectTypeUtil.createObjectRefWithFullObject(focus, PrismContext.get());
        } else {
            ref = ObjectTypeUtil.createObjectRef(focus, PrismContext.get());
        }
        aCase.setObjectRef(ref);
    }

    public void setTargetRef(ObjectReferenceType ref, OperationResult result) {
        ref = getChangeProcessor().getMiscHelper().resolveObjectReferenceName(ref, result);
        aCase.setTargetRef(ref);
    }

    protected void setRequesterRef(PrismObject<? extends FocusType> requester) {
        aCase.setRequestorRef(createObjectRef(requester, PrismContext.get()));
    }

    public ApprovalContextType getApprovalContext() {
        return aCase.getApprovalContext();
    }

    //endregion

    //region Diagnostics

    @Override
    public String toString() {
        return "StartInstruction{" +
                "aCase=" + aCase +
                ", changeProcessor=" + changeProcessor +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Start instruction: ");
        sb.append(startsWorkflowProcess() ? "with-process" : "no-process").append(", model-context: ");
        sb.append(aCase.getModelContext() != null ? "YES" : "no").append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("Case:\n");
        sb.append(aCase.asPrismContainerValue().debugDump(indent+2)).append("\n");
        return sb.toString();
    }

    public void setParent(CaseType parent) {
        aCase.setParentRef(ObjectTypeUtil.createObjectRef(parent, PrismContext.get()));
    }
    //endregion

    //region "Output" methods
    public CaseType getCase() {
        if (startsWorkflowProcess()) {
            // These cases will be open explicitly using the workflow engine
            aCase.setState(SchemaConstants.CASE_STATE_CREATED);
        } else {
            aCase.setState(SchemaConstants.CASE_STATE_OPEN);
        }
        return aCase;
    }
    //endregion
}
