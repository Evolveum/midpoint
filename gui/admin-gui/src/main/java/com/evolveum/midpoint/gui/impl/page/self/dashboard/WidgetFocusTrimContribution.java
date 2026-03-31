/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable contribution assembled for a single supported self-dashboard widget while trim rules are being derived.
 *
 * <p>Instances are populated by {@link DashboardWidgetTrimRegistry} for registered assignment-backed panels and then
 * merged into {@link DashboardWidgetRuntimeInspector.TrimPlan}. Unsupported panels return no contribution, which keeps
 * trimming disabled for them and preserves correctness.</p>
 */
public class WidgetFocusTrimContribution {

    final List<AssignmentPanelRule> assignmentPanelRules = new ArrayList<>();
    boolean keepAssignmentsMatchingRoleMembershipRef;

    public boolean isEmpty() {
        return assignmentPanelRules.isEmpty()
                && !keepAssignmentsMatchingRoleMembershipRef;
    }

    /**
     * Adds an assignment-preserving rule for a preview widget.
     */
    public void addAssignmentPanelRule(String panelType, int limit, QName targetType) {
        assignmentPanelRules.add(new AssignmentPanelRule(panelType, limit, targetType));
    }

    /**
     * Adds a rule that preserves all assignments matching the widget semantics.
     */
    public void addPreservedAssignmentPanelRule(String panelType, QName targetType) {
        assignmentPanelRules.add(new AssignmentPanelRule(panelType, 0, targetType).withPreserveAll(true));
    }

    /**
     * Preserves assignments whose target matches {@code roleMembershipRef} entries used by non-assignment-backed
     * access widgets such as {@code userAllAccesses}.
     */
    public void setKeepAssignmentsMatchingRoleMembershipRef(boolean keepAssignmentsMatchingRoleMembershipRef) {
        this.keepAssignmentsMatchingRoleMembershipRef = keepAssignmentsMatchingRoleMembershipRef;
    }
}
