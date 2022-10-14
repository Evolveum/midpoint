/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectPolicyDetailsPanel extends MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> {

    private static final ItemPath[] MANDATORY_OVERRIDE_PATHS = {
            ItemPath.create(
                    SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                    ObjectPolicyConfigurationType.F_CONFLICT_RESOLUTION,
                    ConflictResolutionType.F_ACTION),
            ItemPath.create(
                    SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                    ObjectPolicyConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                    ArchetypeAdminGuiConfigurationType.F_OBJECT_DETAILS,
                    GuiObjectDetailsPageType.F_TYPE)
    };

    public ObjectPolicyDetailsPanel(String id, IModel<PrismContainerValueWrapper<ObjectPolicyConfigurationType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public ObjectPolicyDetailsPanel(String id, IModel<PrismContainerValueWrapper<ObjectPolicyConfigurationType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<ObjectPolicyConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("ObjectPolicyConfigurationType.label");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> {
                    ObjectPolicyConfigurationType opc = getModelObject();

                    return StringUtils.joinWith("/", opc.getType(), opc.getSubtype());
                };
            }

            @Override
            protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
                WebMarkupContainer c = new WebMarkupContainer(idTypeImage);
                c.setVisible(false);
                return c;
            }
        };

        return d;
    }

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
}
