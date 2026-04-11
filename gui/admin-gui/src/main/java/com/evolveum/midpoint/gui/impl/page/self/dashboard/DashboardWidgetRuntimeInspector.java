/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HomePageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PreviewContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Derives and applies self-dashboard focus trimming for supported assignment-backed widgets.
 *
 * <p>The inspector looks at configured dashboard widgets, asks {@link DashboardWidgetTrimRegistry} for trim semantics
 * of supported registered panels, and pre-trims {@link UserType#F_ASSIGNMENT} before expensive wrapper creation starts.
 * Unsupported custom panels are intentionally ignored: the dashboard remains correct, only unoptimized.</p>
 */
public final class DashboardWidgetRuntimeInspector {

    private static final String LINK_WIDGET_IDENTIFIER = "linkWidget";

    private DashboardWidgetRuntimeInspector() {
    }

    /**
     * Combined trimming plan derived from all supported widgets on the self dashboard.
     */
    public static final class TrimPlan {

        final List<AssignmentPanelRule> assignmentPanelRules = new java.util.ArrayList<>();
        boolean keepAssignmentsMatchingRoleMembershipRef;

        public boolean enabled() {
            return !assignmentPanelRules.isEmpty() || keepAssignmentsMatchingRoleMembershipRef;
        }

        /**
         * Applies the derived trim plan to the cloned self object before wrapper creation.
         */
        public void trimFocus(PageBase pageBase, UserType user) {
            if (user == null || !enabled()) {
                return;
            }

            trimAssignments(pageBase, user);
        }

        private void trimAssignments(PageBase pageBase, UserType user) {
            List<AssignmentType> assignments = user.getAssignment();
            if (CollectionUtils.isEmpty(assignments)) {
                return;
            }

            Set<AssignmentType> allowedAssignments = new LinkedHashSet<>();
            for (AssignmentPanelRule rule : assignmentPanelRules) {
                List<AssignmentType> filtered = filterAssignments(pageBase, assignments, rule);
                if (rule.preserveAll()) {
                    allowedAssignments.addAll(filtered);
                } else {
                    int effectiveLimit = Math.min(rule.limit(), filtered.size());
                    allowedAssignments.addAll(filtered.subList(0, effectiveLimit));
                }
            }

            if (keepAssignmentsMatchingRoleMembershipRef && CollectionUtils.isNotEmpty(user.getRoleMembershipRef())) {
                addAssignmentsMatchingRoleMembershipRef(user, assignments, allowedAssignments);
            }

            if (allowedAssignments.isEmpty()) {
                assignments.clear();
                return;
            }

            assignments.removeIf(assignment -> !allowedAssignments.contains(assignment));
        }

        private void addAssignmentsMatchingRoleMembershipRef(
                UserType user,
                List<AssignmentType> assignments,
                Set<AssignmentType> allowedAssignments) {
            for (AssignmentType assignment : assignments) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef == null) {
                    continue;
                }
                boolean match = user.getRoleMembershipRef().stream()
                        .anyMatch(roleMembershipRef ->
                                roleMembershipRef != null
                                        && QNameUtil.match(roleMembershipRef.getType(), targetRef.getType())
                                        && roleMembershipRef.getOid() != null
                                        && roleMembershipRef.getOid().equals(targetRef.getOid()));
                if (match) {
                    allowedAssignments.add(assignment);
                }
            }
        }
    }

    /**
     * Builds a trim plan for the current home page by inspecting only widgets backed by supported registered panels.
     */
    public static TrimPlan deriveFocusTrimPlan(PageSelfDashboard page, HomePageType homePage) {
        TrimPlan trimPlan = new TrimPlan();
        if (homePage == null || CollectionUtils.isEmpty(homePage.getWidget())) {
            return trimPlan;
        }

        for (PreviewContainerPanelConfigurationType widget : homePage.getWidget()) {
            if (widget == null || widget.getPanelType() == null || LINK_WIDGET_IDENTIFIER.equals(widget.getPanelType())) {
                continue;
            }

            int limit = getWidgetPreviewLimit(widget);
            CompiledObjectCollectionView collectionView = getCompiledCollectionView(page, widget);
            WidgetFocusTrimContribution contribution = createWidgetContribution(page, widget.getPanelType(), limit);
            if (contribution == null || contribution.isEmpty()) {
                continue;
            }
            mergeWidgetContribution(page, trimPlan, collectionView, contribution);
        }

        return trimPlan;
    }

    public static int getWidgetPreviewLimit(PreviewContainerPanelConfigurationType widget) {
        Integer previewSize = widget.getPreviewSize();
        if (previewSize == null || previewSize <= 0) {
            return UserProfileStorage.DEFAULT_DASHBOARD_PAGING_SIZE;
        }
        return previewSize;
    }

    private static WidgetFocusTrimContribution createWidgetContribution(PageSelfDashboard page, String panelType, int limit) {
        Class<? extends Panel> panelClass = page.findObjectPanel(panelType);
        if (panelClass == null) {
            return null;
        }
        WidgetFocusTrimContribution contribution = DashboardWidgetTrimRegistry.createContribution(panelType, limit, panelClass);
        if (contribution == null) {
            return null;
        }
        contribution.assignmentPanelRules.replaceAll(rule -> rule.withPanelClass(panelClass));
        return contribution;
    }

    private static CompiledObjectCollectionView getCompiledCollectionView(PageSelfDashboard page, PreviewContainerPanelConfigurationType widget) {
        if (widget.getListView() == null) {
            return null;
        }
        return WebComponentUtil.getCompiledObjectCollectionView(widget.getListView(), widget, page);
    }

    private static void mergeWidgetContribution(
            PageSelfDashboard page,
            TrimPlan trimPlan,
            CompiledObjectCollectionView collectionView,
            WidgetFocusTrimContribution contribution) {
        ObjectFilter widgetFilter = getWidgetFilter(page, collectionView);
        if (CollectionUtils.isNotEmpty(contribution.assignmentPanelRules)) {
            List<AssignmentPanelRule> mergedRules = collectionView == null
                    ? contribution.assignmentPanelRules
                    : contribution.assignmentPanelRules.stream()
                            .map(rule -> rule.withAdditionalFilter(widgetFilter))
                            .toList();
            mergedRules.forEach(rule -> mergeAssignmentPanelRule(trimPlan, rule));
        }

        if (contribution.keepAssignmentsMatchingRoleMembershipRef) {
            trimPlan.keepAssignmentsMatchingRoleMembershipRef = true;
        }
    }

    private static ObjectFilter getWidgetFilter(PageSelfDashboard page, CompiledObjectCollectionView collectionView) {
        if (collectionView == null || collectionView.getFilter() == null) {
            return null;
        }
        OperationResult result = new OperationResult(DashboardWidgetRuntimeInspector.class.getSimpleName() + ".evaluateWidgetFilter");
        return WebComponentUtil.evaluateExpressionsInFilter(collectionView.getFilter(), result, page);
    }

    /**
     * Creates the effective assignment query used for trim-time prefiltering.
     */
    public static ObjectQuery createAssignmentQuery(AssignmentPanelRule rule) {
        return DashboardWidgetTrimRegistry.createAssignmentQuery(rule);
    }

    /**
     * Applies the same assignment query semantics used by supported runtime panels to the in-memory assignment list.
     */
    public static List<AssignmentType> filterAssignments(
            PageBase pageBase,
            List<AssignmentType> assignments,
            AssignmentPanelRule rule) {
        if (CollectionUtils.isEmpty(assignments)) {
            return List.of();
        }

        ObjectQuery query = createAssignmentQuery(rule);
        return AbstractAssignmentTypePanel.prefilterAssignmentsUsingQuery(pageBase, assignments, query).stream()
                .filter(assignment -> AbstractAssignmentTypePanel.matchesQuery(pageBase, assignment, rule.additionalFilter()))
                .toList();
    }

    private static void mergeAssignmentPanelRule(TrimPlan trimPlan, AssignmentPanelRule candidate) {
        for (int i = 0; i < trimPlan.assignmentPanelRules.size(); i++) {
            AssignmentPanelRule existing = trimPlan.assignmentPanelRules.get(i);
            if (!isEquivalentRule(existing, candidate)) {
                continue;
            }

            if (candidate.limit() > existing.limit()) {
                trimPlan.assignmentPanelRules.set(i, new AssignmentPanelRule(
                        existing.panelType(),
                        candidate.limit(),
                        existing.targetType(),
                        existing.additionalFilter(),
                        existing.panelClass(),
                        existing.preserveAll()));
            } else if (candidate.preserveAll() && !existing.preserveAll()) {
                trimPlan.assignmentPanelRules.set(i, existing.withPreserveAll(true));
            }
            return;
        }

        trimPlan.assignmentPanelRules.add(candidate);
    }

    private static boolean isEquivalentRule(AssignmentPanelRule left, AssignmentPanelRule right) {
        return Objects.equals(left.panelType(), right.panelType())
                && Objects.equals(left.targetType(), right.targetType())
                && Objects.equals(left.panelClass(), right.panelClass())
                && Objects.equals(getFilterSignature(left.additionalFilter()), getFilterSignature(right.additionalFilter()));
    }

    private static String getFilterSignature(ObjectFilter filter) {
        return filter != null ? filter.debugDump(0) : null;
    }
}
