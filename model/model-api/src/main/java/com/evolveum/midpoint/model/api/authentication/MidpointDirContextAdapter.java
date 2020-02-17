/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import org.springframework.ldap.core.DirContextAdapter;

/**
 * @author skublik
 */

public class MidpointDirContextAdapter extends DirContextAdapter {

    public MidpointDirContextAdapter(DirContextAdapter dirContextAdapter){
        super(dirContextAdapter);
    }

    private String namingAttr;

    public void setNamingAttr(String namingAttr) {
        this.namingAttr = namingAttr;
    }

    public String getNamingAttr() {
        return namingAttr;
    }
}
