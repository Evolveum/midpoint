/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.dto;

import java.io.Serializable;

/**
 * Created by Kate on 23.09.2015.
 */
public class LinkDto implements Serializable {

    private String linkUrl;
    private String linkName;

    public String getLinkUrl() {
        return linkUrl;
    }
}
