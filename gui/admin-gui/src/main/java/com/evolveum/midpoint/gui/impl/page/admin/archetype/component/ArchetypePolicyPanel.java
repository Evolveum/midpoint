/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.archetype.component;

import java.util.Arrays;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "archetypePolicy")
@PanelInstance(identifier = "archetypePolicy",
        applicableForType = ArchetypeType.class,
        display = @PanelDisplay(label = "PageArchetype.archetypePolicy", order = 140))
public class ArchetypePolicyPanel extends AbstractObjectMainPanel<ArchetypeType, FocusDetailsModels<ArchetypeType>> {
    private static final long serialVersionUID = 1L;

    private static final ItemPath[] MANDATORY_OVERRIDE_PATHS = {
            ItemPath.create(
                    ArchetypeType.F_ARCHETYPE_POLICY,
                    ArchetypePolicyType.F_CONFLICT_RESOLUTION,
                    ConflictResolutionType.F_ACTION),
            ItemPath.create(
                    ArchetypeType.F_ARCHETYPE_POLICY,
                    ArchetypePolicyType.F_ADMIN_GUI_CONFIGURATION,
                    ArchetypeAdminGuiConfigurationType.F_OBJECT_DETAILS,
                    GuiObjectDetailsPageType.F_TYPE)
    };

    private static final String ID_PANEL = "panel";

    public ArchetypePolicyPanel(String id, FocusDetailsModels<ArchetypeType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<ArchetypePolicyType>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ArchetypeType.F_ARCHETYPE_POLICY),
                        ArchetypePolicyType.COMPLEX_TYPE) {

                    @Override
                    protected ItemMandatoryHandler getMandatoryHandler() {
                        return itemWrapper -> {
                            ItemPath named = itemWrapper.getPath().namedSegmentsOnly();
                            if (Arrays.stream(MANDATORY_OVERRIDE_PATHS).anyMatch(p -> p.equivalent(named))) {
                                return false;
                            }

                            return itemWrapper.isMandatory();
                        };
                    }
                };
        add(panel);
    }
}
