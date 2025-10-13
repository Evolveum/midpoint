/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 *
 * TODO: refactor for lazy loading
 *
 * @author semancik
 */
public class DisplayNameModel implements IModel<String> {
    private static final long serialVersionUID = 1L;

    private String name;

    public DisplayNameModel(AbstractRoleType role) {
        PolyStringType displayName = role.getDisplayName();
        if (displayName == null) {
            displayName = role.getName();
        }
        if (displayName == null) {
            name = "";
        } else {
            name = displayName.getOrig();
        }
    }

    @Override
    public void detach() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getObject() {
        return name;
    }

    @Override
    public void setObject(String object) {
        this.name = object;
    }

}
