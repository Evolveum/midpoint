/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collection;

public abstract class SelectableObjectModel<O extends ObjectType> extends LoadableDetachableModel<O> implements SelectableRowModel<O> {

    private String oid;
    private Class<O> type;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    private boolean selected;

    public SelectableObjectModel(O object, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(object);
        this.options = options;
        this.oid = object.getOid();
        this.type = (Class<O>) object.getClass();
    }

//    @Override
//    protected O load() {
//        PageBase pageBase = getPageBase();
//        Task task = pageBase.createSimpleTask("load object");
//        OperationResult result = task.getResult();
//        PrismObject<O> object = WebModelServiceUtils.loadObject(type, oid, pageBase, task, result);
//        result.computeStatusIfUnknown();
//        SelectableBeanImpl selectableBean;
//        if (object != null) {
//             selectableBean = new SelectableBeanImpl<>(object.asObjectable());
//            selectableBean.setSelected(selected);
//        } else {
//            selectableBean = new SelectableBeanImpl<>(null);
//        }
//        selectableBean.setResult(result);
////        return selectableBean;
//        return object.asObjectable();
//    }

    @Override
    protected void onDetach() {
        if (isAttached()) {
            O object = getObject();
//            selected = seletableBean.isSelected();
//            O object = seletableBean.getValue();
            if (object != null) {
                oid = object.getOid();
                type = (Class<O>) object.getClass();
            }
        }
    }

//    protected PageBase getPageBase() {
//        throw new UnsupportedOperationException("Must be implemented in caller.");
//    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getOid() {
        if (isAttached()) {
            getObject().getOid();
        }
        return oid;
    }

    public Class<O> getType() {
        if (isAttached()) {
            getObject().getClass();
        }
        return type;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }
}
