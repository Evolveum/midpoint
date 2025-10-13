/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.io.Serializable;

/**
 * The RoleAnalysisModel class stores role analysis data, count of roles and users that are used in the histogram chart.
 * It displays the number of grouped roles with same number of users and the number of users that are assigned to the roles.
 */
public class RoleAnalysisModel implements Serializable {

    int rolesCount;
    int usersCount;

    public RoleAnalysisModel(int rolesCount, int usersCount) {
        this.rolesCount = rolesCount;
        this.usersCount = usersCount;
    }

    public int getRolesCount() {
        return rolesCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

}
