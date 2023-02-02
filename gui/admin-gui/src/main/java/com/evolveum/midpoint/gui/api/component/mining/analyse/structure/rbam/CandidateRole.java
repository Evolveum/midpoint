/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam;

import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import java.util.List;

public class CandidateRole {

    List<AuthorizationType> rolePermission;
    double support;
    double actualSupport;
    int degree;
    int key;
    boolean active;
    List<UA> users;
    List<UPStructure> supportUsers;
    boolean sufficientSupport;

    public CandidateRole(List<AuthorizationType> rolePermission,
            double support, double actualSupport, int degree, int key, boolean active, boolean sufficientSupport,
            List<UA> users, List<UPStructure> supportUsers) {
        this.rolePermission = rolePermission;
        this.support = support;
        this.actualSupport = actualSupport;
        this.degree = degree;
        this.key = key;
        this.active = active;
        this.sufficientSupport = sufficientSupport;
        this.users = users;
        this.supportUsers = supportUsers;
    }


    public List<AuthorizationType> getRolePermission() {
        return rolePermission;
    }

    public void setRolePermission(List<AuthorizationType> rolePermission) {
        this.rolePermission = rolePermission;
    }

    public double getSupport() {
        return support;
    }

    public void setSupport(double support) {
        this.support = support;
    }

    public double getActualSupport() {
        return actualSupport;
    }

    public void setActualSupport(double actualSupport) {
        this.actualSupport = actualSupport;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<UA> getUsers() {
        return users;
    }

    public void setUsers(List<UA> users) {
        this.users = users;
    }

    public boolean isSufficientSupport() {
        return sufficientSupport;
    }

    public void setSufficientSupport(boolean sufficientSupport) {
        this.sufficientSupport = sufficientSupport;
    }

    public List<UPStructure> getSupportUsers() {
        return supportUsers;
    }

    public void setSupportUsers(List<UPStructure> supportUsers) {
        this.supportUsers = supportUsers;
    }

}
