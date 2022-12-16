/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleInducementPanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import java.util.List;

@PanelType(name = "focusMappingsInducements")
@PanelInstance(identifier = "focusMappingsInducements",
        applicableForType = AbstractRoleType.class,
        childOf = AbstractRoleInducementPanel.class,
        display = @PanelDisplay(label = "AssignmentType.focusMappings", order = 80))
public class FocusMappingsInducementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    public FocusMappingsInducementsPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected ObjectQuery createCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS).build();
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        // CustomizeQuery is not repo indexed
        if (isRepositorySearchEnabled()) {
            return null;
        }
        return createCustomizeQuery();
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(
            List<PrismContainerValueWrapper<AssignmentType>> list) {
        // customizeQuery is not repository supported, so we need to prefilter list using in-memory search
        if (isRepositorySearchEnabled()) {
            return prefilterUsingQuery(list, createCustomizeQuery());
        }
        return super.customPostSearch(list);
    }
}
