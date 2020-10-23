/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.service;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.page.AbstractRolePage;

/**
 * Created by honchar
 */
public class ServicePage extends AbstractRolePage {

    @Override
    public ProjectionsTab<ServicePage> selectTabProjections() {
        return super.selectTabProjections();
    }

    @Override
    public AssignmentHolderBasicTab<ServicePage> selectTabBasic() {
        return super.selectTabBasic();
    }

    @Override
    public AssignmentsTab<ServicePage> selectTabAssignments() {
        return super.selectTabAssignments();
    }

}
