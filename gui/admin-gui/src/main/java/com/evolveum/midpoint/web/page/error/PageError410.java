/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/error/410", permitAll = true)
public class PageError410 extends PageError {

    public PageError410() {
        super(410);
    }
}
