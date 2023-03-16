/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdministrativeAvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ResourceSummaryPanel extends ObjectSummaryPanel<ResourceType> {
    private static final long serialVersionUID = 1L;

    public ResourceSummaryPanel(String id, IModel<ResourceType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, ResourceType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected List<SummaryTag<ResourceType>> getSummaryTagComponentList(){
        AvailabilityStatusType availability = ResourceTypeUtil.getLastAvailabilityStatus(getModelObject());
        AdministrativeAvailabilityStatusType administrativeAvailability = ResourceTypeUtil.getAdministrativeAvailabilityStatus(getModelObject());

        List<SummaryTag<ResourceType>> summaryTagList = new ArrayList<>();

        SummaryTag<ResourceType> summaryTag = new SummaryTag<ResourceType>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(ResourceType object) {
                if (AdministrativeAvailabilityStatusType.MAINTENANCE == administrativeAvailability) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_RESOURCE_MAINTENANCE);
                    setLabel(ResourceSummaryPanel.this.getString(administrativeAvailability));
                    return;
                }

                if (availability == null) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_RESOURCE_UNKNOWN);
                    setLabel(getString("ResourceSummaryPanel.UNKNOWN"));
                    return;
                } else {
                    setLabel(ResourceSummaryPanel.this.getString(availability));
                    switch (availability) {
                        case UP:
                            setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
                            break;
                        case DOWN:
                            setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
                            break;
                        case BROKEN:
                            setIconCssClass(GuiStyleConstants.CLASS_ICON_RESOURCE_BROKEN);
                            break;

                    }
                }
            }
        };
        summaryTagList.add(summaryTag);

        SummaryTag<ResourceType> modeTag = new SummaryTag<ResourceType>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(ResourceType object) {
                String lifecycleState = getModelObject().getLifecycleState();
                if (StringUtils.isEmpty(lifecycleState)) {
                    lifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
                }

                if (!SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycleState)
                        && !SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycleState)) {
                    setHideTag(true);
                    return;
                } else {
                    setHideTag(false);
                }

                String mode = ResourceSummaryPanel.this.getString("SimulationMode." + lifecycleState);
                setLabel(ResourceSummaryPanel.this.getString("ObjectSummaryPanel.mode", mode));
                setIconCssClass("fa fa-dna");
            }
        };
        summaryTagList.add(modeTag);
        return summaryTagList;
    }


    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-resource";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-resource";
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
