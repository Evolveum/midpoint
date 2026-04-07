/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class WidgetFocusTrimContribution {

    final List<AssignmentPanelRule> assignmentPanelRules = new ArrayList<>();
    boolean keepAssignmentsMatchingRoleMembershipRef;

    public boolean isEmpty() {
        return assignmentPanelRules.isEmpty()
                && !keepAssignmentsMatchingRoleMembershipRef;
    }

    public void addAssignmentPanelRule(String panelType, int limit, QName targetType) {
        assignmentPanelRules.add(new AssignmentPanelRule(panelType, limit, targetType));
    }

    public void addPreservedAssignmentPanelRule(String panelType, QName targetType) {
        assignmentPanelRules.add(new AssignmentPanelRule(panelType, 0, targetType).withPreserveAll(true));
    }

    public void setKeepAssignmentsMatchingRoleMembershipRef(boolean keepAssignmentsMatchingRoleMembershipRef) {
        this.keepAssignmentsMatchingRoleMembershipRef = keepAssignmentsMatchingRoleMembershipRef;
    }
}
