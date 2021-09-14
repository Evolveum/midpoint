/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.archetype.component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ContainerOfSystemConfigurationPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "archetypePolicy")
@PanelInstance(identifier = "archetypePolicy",
        applicableFor = ArchetypeType.class,
        status = ItemStatus.NOT_CHANGED,
        display = @PanelDisplay(label = "PageArchetype.archetypePolicy", order = 140))
public class ArchetypePolicyPanel extends AbstractObjectMainPanel<ArchetypeType, FocusDetailsModels<ArchetypeType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypePolicyPanel.class);
    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = ArchetypePolicyPanel.class.getName() + ".";

    public ArchetypePolicyPanel(String id, FocusDetailsModels<ArchetypeType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        ContainerOfSystemConfigurationPanel panel =
                new ContainerOfSystemConfigurationPanel<ArchetypePolicyType>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ArchetypeType.F_ARCHETYPE_POLICY),
                        ArchetypePolicyType.COMPLEX_TYPE);
        add(panel);
    }

}
