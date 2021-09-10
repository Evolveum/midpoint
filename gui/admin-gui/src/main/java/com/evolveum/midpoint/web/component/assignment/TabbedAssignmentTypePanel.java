/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

public class TabbedAssignmentTypePanel extends TabbedPanel<ITab> {

    private ContainerPanelConfigurationType config;

    public TabbedAssignmentTypePanel(String id, List<ITab> tabs, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel,
            ContainerPanelConfigurationType config) {
        super(id, tabs);
        this.config = config;
    }

}
