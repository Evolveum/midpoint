/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */
public class ObjectBasicPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectBasicPanel.class);

    private static final String ID_BASIC_CONTAINER = "basicObjectContainer";


    public ObjectBasicPanel(String id, IModel<PrismObjectWrapper<O>> model) {
        super(id, model);

        setOutputMarkupId(true);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    protected void initLayout() {
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(this::getBasicTabVisibility);
            Panel panel = getPageBase().initItemPanel(ID_BASIC_CONTAINER, getType(), getModel(), builder.build());
            add(panel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create basic panel for system configuration.");
            getSession().error("Cannot create basic panel for system configuration.");
        }

    }

    protected QName getType() {
        return ObjectType.COMPLEX_TYPE;
    }

    protected ItemVisibility getBasicTabVisibility(ItemWrapper<?, ?> itemWrapper) {
        return ItemVisibility.AUTO;
    }
}
