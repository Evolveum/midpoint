/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

public class MMiningObject extends MObject {

    public String identifier;
    public String riskLevel;
    public Integer rolesCount;
    public Integer membersCount;
    public Integer similarGroupsCount;
    public String[] roles;
    public String[] members;
    public String[] similarGroups;

}
