/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.ldap.core.DirContextAdapter;

import java.util.List;

/**
 * @author skublik
 */

public class MidpointDirContextAdapter extends DirContextAdapter {

    public MidpointDirContextAdapter(DirContextAdapter dirContextAdapter){
        super(dirContextAdapter);
    }

    private String namingAttr;
    private Class<? extends FocusType> focusType = UserType.class;
    private List<ObjectReferenceType> requireAssignment = null;
    private AuthenticationChannel channel = null;
    private ConnectionEnvironment connectionEnvironment = null;

    public void setNamingAttr(String namingAttr) {
        this.namingAttr = namingAttr;
    }

    public String getNamingAttr() {
        return namingAttr;
    }

    public void setFocusType(Class<? extends FocusType> focusType) {
        this.focusType = focusType;
    }

    public Class<? extends FocusType> getFocusType() {
        return focusType;
    }

    public List<ObjectReferenceType> getRequireAssignment() {
        return requireAssignment;
    }

    public void setRequireAssignment(List<ObjectReferenceType> requireAssignment) {
        this.requireAssignment = requireAssignment;
    }

    public AuthenticationChannel getChannel() {
        return channel;
    }

    public void setChannel(AuthenticationChannel channel) {
        this.channel = channel;
    }

    public ConnectionEnvironment getConnectionEnvironment() {
        return connectionEnvironment;
    }

    public void setConnectionEnvironment(ConnectionEnvironment connectionEnvironment) {
        this.connectionEnvironment = connectionEnvironment;
    }
}
