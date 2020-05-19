/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

/**
 * @author skublik
 */
public class ContainerOfSystemConfigurationPanel<C extends Containerable> extends SingleContainerPanel<C> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerOfSystemConfigurationPanel.class);

    private static final String ID_CONTAINER = "container";

    public ContainerOfSystemConfigurationPanel(String id, IModel<PrismContainerWrapper<C>> model, QName typeName) {
        super(id, model, typeName);
    }

    protected ItemVisibility getVisibity(ItemPath itemPath) {
        ItemName name = itemPath.firstToNameOrNull();
        if (name == null) {
            return ItemVisibility.HIDDEN;
        }
        if (itemPath.firstToName().equals(SystemConfigurationType.F_WORKFLOW_CONFIGURATION)) {
            if (itemPath.lastName().equals(WfConfigurationType.F_APPROVER_COMMENTS_FORMATTING)) {
                return ItemVisibility.HIDDEN;
            }

            if (itemPath.rest().equivalent(ItemPath.create(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR, PrimaryChangeProcessorConfigurationType.F_ADD_ASSOCIATION_ASPECT,
                    PcpAspectConfigurationType.F_APPROVER_REF))) {
                return ItemVisibility.AUTO;
            }

            if (itemPath.rest().startsWithName(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR)
                    && (itemPath.lastName().equals(PcpAspectConfigurationType.F_APPROVER_EXPRESSION)
                            || itemPath.lastName().equals(PcpAspectConfigurationType.F_APPROVER_REF)
                            || itemPath.lastName().equals(PcpAspectConfigurationType.F_AUTOMATICALLY_APPROVED)
                            || itemPath.lastName().equals(PcpAspectConfigurationType.F_APPLICABILITY_CONDITION))) {
                return ItemVisibility.HIDDEN;
            }
        }

        if (itemPath.equivalent(ItemPath.create(SystemConfigurationType.F_ACCESS_CERTIFICATION, AccessCertificationConfigurationType.F_REVIEWER_COMMENTS_FORMATTING))) {
            return ItemVisibility.HIDDEN;
        }

        return ItemVisibility.AUTO;
    }


}
