/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.TitleWithMarks;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;

/**
 * @author semancik
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractNameColumn<SelectableBean<O>, O> {

    @Serial private static final long serialVersionUID = 1L;

    public ObjectNameColumn(IModel<String> displayModel) {
        this(displayModel, null, null, null);
    }

    public ObjectNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression, PageBase pageBase) {
        super(displayModel, ObjectType.F_NAME.getLocalPart(),  customColumn, expression, pageBase);
    }

    @Override
    protected IModel<String> getContainerName(@NotNull SelectableBean<O> selectableBean) {
        O value = selectableBean.getValue();
        return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
    }

    @Override
    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<O>> rowModel) {
        IModel<String> realMarksModel = () -> createRealMarksList(rowModel.getObject());
        return new TitleWithMarks(componentId, labelModel, realMarksModel) {

            @Override
            protected void onTitleClicked() {
                ObjectNameColumn.this.onClick(rowModel);
            }

            @Override
            protected boolean isTitleLinkEnabled() {
                return ObjectNameColumn.this.isClickable(rowModel);
            }
        };
//        return new LinkPanel(componentId, labelModel) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick() {
//                ObjectNameColumn.this.onClick(rowModel);
//            }
//
//            @Override
//            public boolean isEnabled() {
//                return ObjectNameColumn.this.isClickable(rowModel);
//            }
//        };
    }

    private String createRealMarksList(SelectableBean<O> bean) {
        O object = bean.getValue();
        if (object == null) {
            return null;
        }

        List<ObjectReferenceType> refs = object.getEffectiveMarkRef();
        Object[] marks = refs.stream()
                .map(ref -> WebModelServiceUtils.loadObject(ref, getPageBase()))
                .filter(Objects::nonNull)
                .map(WebComponentUtil::getDisplayNameOrName)
                .toArray();

        return StringUtils.joinWith(", ", marks);
    }

    protected void onClick(IModel<SelectableBean<O>> rowModel) {
    }

    protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }
}
