/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class CandidateRole implements Serializable {

    private int key;
    private List<AuthorizationType> candidatePermissions;
    private List<RoleType> candidateRoles;
    private double actualSupport;
    private double support;
    private boolean active;
    private Set<Integer> childrenKeys;
    private HashMap<Integer, Connection> parentKeys;
    int degree;

    public CandidateRole(int degree, int key, Set<Integer> childrenKeys, HashMap<Integer, Connection> parentKeys,
            double actualSupport, double support, boolean active, List<AuthorizationType> candidatePermissions) {
        this.degree = degree;
        this.key = key;
        this.childrenKeys = childrenKeys;
        this.parentKeys = parentKeys;
        this.candidatePermissions = candidatePermissions;
        this.actualSupport = actualSupport;
        this.support = support;
        this.active = active;
    }

    public CandidateRole(int degree, int key, List<RoleType> candidateRoles, Set<Integer> childrenKeys,
            HashMap<Integer, Connection> parentKeys, double actualSupport, double support, boolean active) {
        this.degree = degree;
        this.key = key;
        this.childrenKeys = childrenKeys;
        this.parentKeys = parentKeys;
        this.candidateRoles = candidateRoles;
        this.actualSupport = actualSupport;
        this.support = support;
        this.active = active;
    }

    public List<RoleType> getCandidateRoles() {
        return candidateRoles;
    }

    public void setCandidateRoles(List<RoleType> candidateRoles) {
        this.candidateRoles = candidateRoles;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public HashMap<Integer, Connection> getParentKeys() {
        return parentKeys;
    }

    public void setParentKeys(HashMap<Integer, Connection> parentKeys) {
        this.parentKeys = parentKeys;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public List<AuthorizationType> getCandidatePermissions() {
        return candidatePermissions;
    }

    public void setCandidatePermissions(List<AuthorizationType> candidatePermissions) {
        this.candidatePermissions = candidatePermissions;
    }

    public double getActualSupport() {
        return actualSupport;
    }

    public void setActualSupport(double actualSupport) {
        this.actualSupport = actualSupport;
    }

    public double getSupport() {
        return support;
    }

    public void setSupport(double support) {
        this.support = support;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Integer> getChildrenKeys() {
        return childrenKeys;
    }

    public void setChildrenKeys(Set<Integer> childrenKeys) {
        this.childrenKeys = childrenKeys;
    }

}
