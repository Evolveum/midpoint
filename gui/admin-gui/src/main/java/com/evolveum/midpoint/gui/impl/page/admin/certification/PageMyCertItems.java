/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
