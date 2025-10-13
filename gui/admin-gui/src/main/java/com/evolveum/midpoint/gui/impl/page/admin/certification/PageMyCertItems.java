/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/decisions", matchUrlForSecurity = "/admin/certification/decisions")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS,
                        label = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS_DESCRIPTION) })
public class PageMyCertItems extends PageCertItems {

    public PageMyCertItems() {
        super();
    }

    @Override
    boolean showOnlyNotDecidedItems() {
        return true;
    }

    @Override
    protected boolean isMyCertItems() {
        return true;
    }
}
