/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;

public class EditOtpPopupPanel<F extends FocusType> extends OtpPopupPanel<F> {

    @Serial private static final long serialVersionUID = 1L;

    public EditOtpPopupPanel(String id, IModel<F> focusModel, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
        super(id, focusModel, model);

        setEditMode(true);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("EditOtpPopupPanel.title");
    }

    @Override
    public IModel<String> getConfirmButtonLabel() {
        return createStringResource("EditOtpPopupPanel.save");
    }
}
