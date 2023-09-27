/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

public class RoleAnalysisModel {

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
