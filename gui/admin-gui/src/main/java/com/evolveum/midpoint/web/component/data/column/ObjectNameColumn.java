/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.TitleWithMarks;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
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
        super(displayModel, sortProperty, customColumn, expression, pageBase);
    }

    @Override
    protected IModel<String> getContainerName(@NotNull SelectableBean<O> selectableBean) {
        O value = selectableBean.getValue();
        return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
    }

    @Override
    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<O>> rowModel) {
        return new TitleWithMarks(componentId, labelModel, createRealMarksList(rowModel.getObject())) {

            @Override
            protected void onTitleClicked() {
                ObjectNameColumn.this.onClick(rowModel);
            }

            @Override
            protected boolean isTitleLinkEnabled() {
                return ObjectNameColumn.this.isClickable(rowModel);
            }

            @Override
            protected IModel<String> createPrimaryMarksTitle() {
                return createStringResource("ObjectNameColumn.objectMarks");
            }
        };
    }

    protected IModel<String> createRealMarksList(SelectableBean<O> bean) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                O object = bean.getValue();
                return WebComponentUtil.createMarkList(object, getPageBase());
            }
        };
    }

    protected void onClick(IModel<SelectableBean<O>> rowModel) {
    }

    protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }
}
