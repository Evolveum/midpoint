/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AssignmentPanelQueries;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AbstractAssignmentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AllAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.ConstructionAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.FocusMappingsAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.GdprAssignmentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.GenericAbstractRoleAssignmentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.OrgAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.RoleAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.ServiceAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.component.AllAccessListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.component.DelegatedToMePanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Explicit registry of built-in self-dashboard panels that support focus trimming.
 *
 * <p>Only panels registered here are allowed to contribute trimming behavior.
 * Unsupported custom panels are intentionally ignored.</p>
 */
final class DashboardWidgetTrimRegistry {

    // Only built-in panels listed here participate in self-dashboard trimming.
    private static final Map<Class<? extends Panel>, Support> SUPPORTED_PANELS = new LinkedHashMap<>();

    static {
        SUPPORTED_PANELS.put(AllAssignmentsPanel.class, new Support(
                    AbstractAssignmentPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null)));
        SUPPORTED_PANELS.put(RoleAssignmentsPanel.class, new Support(
                    RoleAssignmentsPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null)));
        SUPPORTED_PANELS.put(OrgAssignmentsPanel.class, new Support(
                    OrgAssignmentsPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null)));
        SUPPORTED_PANELS.put(ServiceAssignmentsPanel.class, new Support(
                    ServiceAssignmentsPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null)));
        SUPPORTED_PANELS.put(ConstructionAssignmentsPanel.class, new Support(
                    AbstractAssignmentPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.constructions()));
        SUPPORTED_PANELS.put(FocusMappingsAssignmentsPanel.class, new Support(
                    AbstractAssignmentPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.focusMappings()));
        SUPPORTED_PANELS.put(GdprAssignmentPanel.class, new Support(
                    AbstractAssignmentPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.gdprAssignments()));
        SUPPORTED_PANELS.put(GenericAbstractRoleAssignmentPanel.class, new Support(
                    AbstractAssignmentPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.accessOrganizations()));
        SUPPORTED_PANELS.put(DelegatedToMePanel.class, new Support(
                    DelegatedToMePanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.delegatedToMe()));
        SUPPORTED_PANELS.put(AllAccessListPanel.class, new Support(
                    AllAccessListPanel::contributeFocusTrimPlan,
                    rule -> AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null)));
    }

    private DashboardWidgetTrimRegistry() {
    }

    /**
     * Creates trim contribution for a supported built-in panel class.
     */
    static WidgetFocusTrimContribution createContribution(String panelType, int limit, Class<? extends Panel> panelClass) {
        Support support = findSupport(panelClass);
        if (support == null) {
            return null;
        }

        WidgetFocusTrimContribution contribution = new WidgetFocusTrimContribution();
        support.contributor().contribute(contribution, panelType, limit);
        return contribution;
    }

    /**
     * Creates the trim-time assignment query matching the runtime semantics of a supported panel.
     */
    static ObjectQuery createAssignmentQuery(AssignmentPanelRule rule) {
        if (rule != null && rule.panelClass() != null) {
            Support support = findSupport(rule.panelClass());
            if (support != null) {
                return support.queryFactory().create(rule);
            }
        }

        return AssignmentPanelQueries.defaultAssignments(rule != null ? rule.targetType() : null);
    }

    /**
     * Resolves support using assignable matching for registered panel hierarchies.
     */
    private static Support findSupport(Class<? extends Panel> panelClass) {
        if (panelClass == null) {
            return null;
        }

        for (Map.Entry<Class<? extends Panel>, Support> entry : SUPPORTED_PANELS.entrySet()) {
            if (entry.getKey().isAssignableFrom(panelClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private record Support(TrimPlanContributor contributor, AssignmentQueryFactory queryFactory) {
    }

    @FunctionalInterface
    private interface TrimPlanContributor {
        void contribute(WidgetFocusTrimContribution contribution, String panelType, int limit);
    }

    @FunctionalInterface
    private interface AssignmentQueryFactory {
        ObjectQuery create(AssignmentPanelRule rule);
    }
}
