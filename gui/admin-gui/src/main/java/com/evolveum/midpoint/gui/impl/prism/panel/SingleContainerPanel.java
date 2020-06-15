/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

public class SingleContainerPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleContainerPanel.class);

    private static final String ID_CONTAINER = "container";
    private QName typeName = null;

    public SingleContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, QName typeName) {
        super(id, model);
        this.typeName = typeName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(wrapper -> getVisibility(wrapper.getPath()))
                    .editabilityHandler(getEditabilityHandler())
                    .mandatoryHandler(getMandatoryHandler());
            Panel panel = getPageBase().initItemPanel(ID_CONTAINER, getTypeName(), getModel(), builder.build());
            add(panel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}, {}", getTypeName(), e.getMessage(), e);
            getSession().error("Cannot create panel for " + getTypeName()); // TODO opertion result? localization?

        }

    }

    protected QName getTypeName() {
        return typeName;
    }

    protected ItemVisibility getVisibility(ItemPath itemPath) {
        return ItemVisibility.AUTO;
    }

    protected ItemEditabilityHandler getEditabilityHandler() {
        return null;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

}
