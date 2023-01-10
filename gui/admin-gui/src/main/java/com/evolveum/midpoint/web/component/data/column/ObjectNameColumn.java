/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class ObjectNameColumn<O extends ObjectType> extends ContainerableNameColumn<O> {

    private static final long serialVersionUID = 1L;

    public ObjectNameColumn(IModel<String> displayModel) {
        this(displayModel, null, null, null, true);
    }

    public ObjectNameColumn(IModel<String> displayModel, ItemPath itemPath, ExpressionType expression, PageBase pageBase, boolean useDefaultPath) {
        super(displayModel, useDefaultPath ? ObjectType.F_NAME : itemPath, expression, pageBase);
    }

    @Override
    protected IModel<String> getContainerName(@NotNull IModel<SelectableBean<O>> rowModel) {
        SelectableBean<O> selectableBean = rowModel.getObject();
        O value = selectableBean.getValue();
        return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
    }
}
