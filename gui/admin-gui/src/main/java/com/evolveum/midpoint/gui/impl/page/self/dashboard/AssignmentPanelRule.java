/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import org.apache.wicket.markup.html.panel.Panel;

public record AssignmentPanelRule(String panelType, int limit, QName targetType, ObjectFilter additionalFilter,
                                  Class<? extends Panel> panelClass, boolean preserveAll) {

    public AssignmentPanelRule(String panelType, int limit) {
        this(panelType, limit, null, null, null, false);
    }

    public AssignmentPanelRule(String panelType, int limit, QName targetType) {
        this(panelType, limit, targetType, null, null, false);
    }

    public AssignmentPanelRule withAdditionalFilter(ObjectFilter filter) {
        return new AssignmentPanelRule(panelType, limit, targetType, filter, panelClass, preserveAll);
    }

    public AssignmentPanelRule withPanelClass(Class<? extends Panel> panelClass) {
        return new AssignmentPanelRule(panelType, limit, targetType, additionalFilter, panelClass, preserveAll);
    }

    public AssignmentPanelRule withPreserveAll(boolean preserveAll) {
        return new AssignmentPanelRule(panelType, limit, targetType, additionalFilter, panelClass, preserveAll);
    }
}
