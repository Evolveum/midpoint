/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

    public PageAdmin() {
        this(null);
    }

    public PageAdmin(PageParameters parameters) {
        super(parameters);
    }
}
