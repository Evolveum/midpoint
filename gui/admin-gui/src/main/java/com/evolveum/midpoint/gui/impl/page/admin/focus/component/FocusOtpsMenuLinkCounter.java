/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsType;

public class FocusOtpsMenuLinkCounter extends SimpleCounter<AssignmentHolderDetailsModel<FocusType>, FocusType> {

    public FocusOtpsMenuLinkCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<FocusType> model, PageBase pageBase) {
        FocusType focus = model.getObjectType();
        CredentialsType credentials = focus.getCredentials();
        if (credentials == null) {
            return 0;
        }

        OtpCredentialsType otpCredentials = credentials.getOtps();
        if (otpCredentials == null) {
            return 0;
        }

        return otpCredentials.getOtp().size();
    }
}
