/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
