/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractNameColumn<SelectableBean<O>, O> {

    @Serial private static final long serialVersionUID = 1L;

    public ObjectNameColumn(IModel<String> displayModel) {
        this(displayModel, null, null, null);
    }

    public ObjectNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression, PageBase pageBase) {
        this(displayModel, ObjectType.F_NAME.getLocalPart(), customColumn, expression, pageBase);
    }

    public ObjectNameColumn(IModel<String> displayModel, String sortProperty, GuiObjectColumnType customColumn, ExpressionType expression, PageBase pageBase) {
        super(displayModel, sortProperty != null ? sortProperty : ObjectType.F_NAME.getLocalPart(), customColumn, expression, pageBase);
    }

    @Override
    protected IModel<String> getContainerName(@NotNull SelectableBean<O> selectableBean) {
        O value = selectableBean.getValue();
        return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
    }

    @Override
    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<O>> rowModel) {
        return new LinkPanel(componentId, labelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                ObjectNameColumn.this.onClick(rowModel);
            }

            @Override
            public boolean isEnabled() {
                return ObjectNameColumn.this.isClickable(rowModel);
            }
        };
    }

    @Override
    public void populateItem(Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId, IModel<SelectableBean<O>> rowModel) {
        super.populateItem(cellItem, componentId, rowModel);
        cellItem.add(AttributeAppender.append("class", "name-min-width"));
    }

    protected void onClick(IModel<SelectableBean<O>> rowModel) {
    }

    protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }
}
