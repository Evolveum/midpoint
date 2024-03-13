/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;

/**
 * Created by Viliam Repan (lazyman).
 */
@Deprecated
public class DeprecatedPropertyWrapperModel<C extends Containerable, P> implements IModel<PrismPropertyWrapper<P>> {

    private BasePanel panel;

    private IModel<PrismContainerWrapper<C>> model;

    private ItemName propertyName;

    public DeprecatedPropertyWrapperModel(BasePanel panel, IModel<PrismContainerWrapper<C>> model, ItemName propertyName) {
        this.panel = panel;
        this.model = model;
        this.propertyName = propertyName;
    }

    @Override
    public PrismPropertyWrapper getObject() {
        PrismContainerWrapper container = model.getObject();

        try {
            PrismContainerValueWrapper value = (PrismContainerValueWrapper) container.getValue();
            PrismPropertyWrapper fileConfiguration = value.findProperty(propertyName);
            if (fileConfiguration != null) {
                return fileConfiguration;
            }

            PrismPropertyDefinition def = container.findPropertyDefinition(ItemPath.create(propertyName));
            ItemWrapperFactory factory = panel.getPageBase().getRegistry().findWrapperFactory(def, value.getNewValue());

            Task task = panel.getPageBase().createSimpleTask("Create child containers");
            WrapperContext ctx = new WrapperContext(task, task.getResult());
            ctx.setCreateIfEmpty(true);
            ctx.setDeprecatedItemAllowed(true);

            PrismPropertyWrapper child = (PrismPropertyWrapper) factory.createWrapper(value, def, ctx);
            value.addItem(child);

            return child;
        } catch (SchemaException ex) {
            throw new SystemException(ex);
        }
    }
}
