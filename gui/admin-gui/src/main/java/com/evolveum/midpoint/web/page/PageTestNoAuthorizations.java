/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

/**
 * Page with no authorizations. It is used for testing: to make sure that nobody can access this page.
 */
@PageDescriptor(urls = {@Url (mountUrl = "/noautz", matchUrlForSecurity = "/noautz")})
public class PageTestNoAuthorizations extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageTestNoAuthorizations.class);

    public PageTestNoAuthorizations() {
    }

    @Override
    protected void createBreadcrumb() {
    }

}
