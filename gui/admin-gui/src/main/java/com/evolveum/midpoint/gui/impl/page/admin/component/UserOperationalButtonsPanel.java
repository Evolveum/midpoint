/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserOperationalButtonsPanel extends FocusOperationalButtonsPanel<UserType> {
    private static final long serialVersionUID = 1L;

    private LoadableModel<List<AssignmentEditorDto>> delegationsModel;

    public UserOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<UserType>> model, LoadableModel<List<AssignmentEditorDto>> delegationsModel, LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel, boolean isSelfProfile) {
        super(id, model, executeOptionsModel, isSelfProfile);
        this.delegationsModel = delegationsModel;
    }

    protected boolean isSavePreviewButtonEnabled() {
        //in case user isn't allowed to modify focus data but has
        // e.g. #assign authorization, Save button is disabled on page load.
        // Save button becomes enabled if some changes are made
        // on the Delegations panel
        return isDelegationAddedOrRemoved() || super.isSavePreviewButtonEnabled();
    }


    //when the user has just #assign authorization (no #edit), we need to enable Save/Preview buttons
    // when the delegations model is changed
    public boolean isDelegationAddedOrRemoved() {
        for (AssignmentEditorDto dto : delegationsModel.getObject()) {
            if (UserDtoStatus.ADD.equals(dto.getStatus()) || UserDtoStatus.DELETE.equals(dto.getStatus())) {
                return true;
            }
        }
        return false;
    }

}
