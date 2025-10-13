/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/error/403")
        },
        permitAll = true)
public class PageError403 extends PageError {

    public PageError403() {
        super(403);
    }

    @Override
    protected String getErrorMessageKey(){
        return "PageError403.message";
    }
}
