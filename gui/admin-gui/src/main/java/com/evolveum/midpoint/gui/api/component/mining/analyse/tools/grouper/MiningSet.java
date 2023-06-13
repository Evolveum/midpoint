/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MiningSet extends SelectableBeanImpl<MiningSet> implements Serializable {
    public static final String F_RATION = "groupOverlapRation";
    public static final String F_GROUP = "groupIdentifier";
    public static final String F_USER_SIZE = "users";
    public static final String F_ID = "id";
    public static final String F_ROLES_BY_TRESHOLD = "rolesByThresholdId";

    int groupIdentifier;
    double groupOverlapRation;
    List<String> roles;
    List<PrismObject<UserType>> users;
    List<Integer> rolesByThresholdId;
    private boolean selected;

    public String getId() {
        return id;
    }

    String id;

    public MiningSet(String id, int groupIdentifier, double groupOverlapRation, List<String> roles,
            List<Integer> rolesByThresholdId, List<PrismObject<UserType>> users) {
        this.groupIdentifier = groupIdentifier;
        this.groupOverlapRation = groupOverlapRation;
        this.roles = roles;
        this.rolesByThresholdId = rolesByThresholdId;
        this.users = users;
        this.id = id;

    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getGroupIdentifier() {
        return groupIdentifier;
    }

    public double getGroupOverlapRation() {
        return groupOverlapRation;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<PrismObject<UserType>> getUsers() {
        return users;
    }

    public List<Integer> getRolesByThresholdId() {
        return rolesByThresholdId;
    }
}
