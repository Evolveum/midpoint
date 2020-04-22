/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class SystemConfigPanel extends BasePanel<PrismObjectWrapper<SystemConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigPanel.class);

    private static final String ID_SYSTEM_CONFIG = "basicSystemConfiguration";


    public SystemConfigPanel(String id, IModel<PrismObjectWrapper<SystemConfigurationType>> model) {
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
            ItemHeaderPanel.ItemPanelSettingsBuilder builder = new ItemHeaderPanel.ItemPanelSettingsBuilder().visibilityHandler(this::getBasicTabVisibity);
            Panel panel = getPageBase().initItemPanel(ID_SYSTEM_CONFIG, SystemConfigurationType.COMPLEX_TYPE, getModel(), builder.build());
            add(panel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create basic panel for system configuration.");
            getSession().error("Cannot create basic panel for system configuration.");
        }

    }

    private ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
        if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, SystemConfigurationType.F_DESCRIPTION)) || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(
                ItemPath.EMPTY_PATH, SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF))) {
            return ItemVisibility.AUTO;
        }

        if(itemWrapper.getPath().isSuperPathOrEquivalent(ItemPath.create(ObjectType.F_EXTENSION))) {
            return ItemVisibility.AUTO;
        }

        return ItemVisibility.HIDDEN;
    }
}
