/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;


import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserOperationalButtonsPanel extends FocusOperationalButtonsPanel<UserType> {
    private static final long serialVersionUID = 1L;

    public UserOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<UserType>> model, LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel, boolean isSelfProfile) {
        super(id, model, executeOptionsModel, isSelfProfile);
    }

    @Override
    protected boolean isAuthorizedToModify() {
        try {
            boolean thisObjectModify = super.isAuthorizedToModify();
            boolean otherUserModify = getPageBase().isAuthorized(ModelAuthorizationAction.MODIFY.getUrl(),
                    AuthorizationPhaseType.EXECUTION, new UserType().asPrismObject(), null, null);
            return thisObjectModify || otherUserModify;
        } catch (Exception e) {
            return false;
        }
    }
}
