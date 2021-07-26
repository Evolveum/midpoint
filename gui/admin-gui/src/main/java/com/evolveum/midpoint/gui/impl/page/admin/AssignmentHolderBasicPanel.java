/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDescription;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

@PanelDescription(identifier = "basic", applicableFor = AssignmentHolderType.class, label = "Basic", icon = GuiStyleConstants.CLASS_CIRCLE_FULL)
public class AssignmentHolderBasicPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<PrismObjectWrapper<AH>> {

    private static final String ID_MAIN_PANEL = "properties";
    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderBasicPanel.class);

    public AssignmentHolderBasicPanel(String id, LoadableModel<PrismObjectWrapper<AH>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        try {

            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(w -> ItemVisibility.AUTO);
            builder.headerVisibility(false);

            Panel main = getPageBase().initItemPanel(ID_MAIN_PANEL, getModelObject().getTypeName(),
                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.EMPTY_PATH), builder.build());
            add(main);

        } catch (SchemaException e) {
            LOGGER.error("Could not create focus details panel. Reason: {}", e.getMessage(), e);
        }
    }


}
