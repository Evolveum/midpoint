/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.web.security.MidPointApplication;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 *
 * Wrapper for AssignmentHolderType. Modify object delta.
 * Removes `parentOrgRef` values from a to-be-created object that should not be there, because of the object's inactivity.
 */
public class AssignmentHolderWrapper<AH extends AssignmentHolderType> extends PrismObjectWrapperImpl<AH> {

    private static final long serialVersionUID = 1L;

    public AssignmentHolderWrapper(PrismObject<AH> item, ItemStatus status) {
        super(item, status);
    }

    @Override
    public ObjectDelta<AH> getObjectDelta() throws CommonException {
        ObjectDelta<AH> delta = super.getObjectDelta();
        if (delta.isAdd()) {
            removeExtraParentOrgRef(delta.getObjectToAdd().asObjectable());
        }
        return delta;
    }

    /**
     * Removes `parentOrgRef` values from a to-be-created object that should not be there, because of the object's inactivity.
     *
     * == Rationale
     *
     * Because of the authorization requirements (both when the object editing starts - see edit schema,
     * and when the operation is submitted to execution), we manually set-up a `parentOrgRef` value for each org assignment
     * that is pre-created in the new object. This is typically when the object is created as a child in a given org via GUI:
     * The GUI creates the respective org assignment and `parentOrgRef` value.
     *
     * (Note that this is not wholly correct, and is more a workaround than a real solution. The real solution would be
     * to either call the projector to do this, or to change the authorization evaluation to not depend on `parentOrgRef`.
     * That way or another, it is currently so. See also MID-3234.)
     *
     * The problem occurs when the object is changed to `draft` or similar LC state during editing. The added `parentOrgRef`
     * should no longer be there, as the current implementation is that the org assignment(s) are inactive in such object LC
     * states (unless the state model says otherwise). If they are present and should not be, the model refuses
     * the ADD operation (see MID-9264). So this method removes them.
     *
     * == Limitations
     *
     * We do NOT treat general cases here, like when the respective assignment itself is modified (e.g.,
     * disabled, validity changed, LC state changed, etc.). We only treat the case when the object as a whole is
     * put into "assignments inactive" LC state.
     */
    private void removeExtraParentOrgRef(@NotNull AH object) throws ConfigurationException {
        SystemConfigurationType config = MidPointApplication.get().getSystemConfigurationIfAvailable();
        LifecycleStateModelType objectStateModel =
                ArchetypeManager.determineLifecycleModel(object.asPrismObject(), config);

        // As for the task execution mode is concerned, we are interested only in whether production or development config
        // is to be used. Currently, all GUI "object details page" actions are carried out in production mode. So we can
        // safely use TaskExecutionMode.PRODUCTION here.
        boolean assignmentsActive = MidPointApplication.get().getActivationComputer().lifecycleHasActiveAssignments(
                object.getLifecycleState(), objectStateModel, TaskExecutionMode.PRODUCTION);

        if (!assignmentsActive) {
            // We assume that this is a new object. All parentOrgRef values should come through assignments.
            // Hence, if assignments are not active, we can remove all such values.
            object.getParentOrgRef().clear();
        }
    }
}
