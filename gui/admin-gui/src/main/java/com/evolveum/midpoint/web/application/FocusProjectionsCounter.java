/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusProjectionsCounter<F extends FocusType> extends SimpleCounter<FocusDetailsModels<F>, F> {

    public FocusProjectionsCounter() {
        super();
    }

    @Override
    public int count(FocusDetailsModels<F> objectDetailsModels, PageBase pageBase) {
        if (objectDetailsModels.getProjectionModel().isAttached()) {
            return objectDetailsModels
                    .getProjectionModel()
                    .getObject()
                    .stream()
                    .filter(shadowWrapper -> !isNewlyAddedShadow(shadowWrapper) && !shadowWrapper.isDead())
                    .toList()
                    .size();
        }

        PrismObjectWrapper<F> assignmentHolderWrapper = objectDetailsModels.getObjectWrapperModel().getObject();
        F object = assignmentHolderWrapper.getObject().asObjectable();

        return ProvisioningObjectsUtil.countLinkForNonDeadShadows(object.getLinkRef());
    }

    //check if the shadow was newly added
    //if true, then it is not counted
    private boolean isNewlyAddedShadow(ShadowWrapper shadowWrapper) {
        return ItemStatus.ADDED.equals(shadowWrapper.getStatus());
    }
}
