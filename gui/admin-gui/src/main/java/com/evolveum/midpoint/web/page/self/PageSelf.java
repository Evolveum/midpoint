/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author Viliam Repan (lazyman)
 */
public class PageSelf extends PageBase {

    private static final long serialVersionUID = 1L;

    public static final String AUTH_SELF_ALL_URI = AuthorizationConstants.AUTZ_UI_SELF_ALL_URL;
    public static final String AUTH_SELF_ALL_LABEL = "PageSelf.auth.selfAll.label";
    public static final String AUTH_SELF_ALL_DESCRIPTION = "PageSelf.auth.selfAll.description";

    public PageSelf() {
    }

    public PageSelf(PageParameters parameters) {
        super(parameters);
    }
}
