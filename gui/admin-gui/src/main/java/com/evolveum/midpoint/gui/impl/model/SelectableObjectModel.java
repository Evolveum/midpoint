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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.LoadableDetachableModel;

public class SelectableObjectModel<O extends ObjectType> extends LoadableDetachableModel<SelectableBean<O>> {

    private String oid;
    private Class<O> type;

    public SelectableObjectModel(SelectableBean<O> object) {
        super(object);
    }

    @Override
    protected SelectableBean<O> load() {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("load object");
        OperationResult result = task.getResult();
        PrismObject<O> object = WebModelServiceUtils.loadObject(type, oid, pageBase, task, result);
        if (object != null) {
            new SelectableBeanImpl<>(object.asObjectable());
        }
        return new SelectableBeanImpl<>(null);
    }

    @Override
    protected void onDetach() {
        if (isAttached()) {
            SelectableBean<O> seletableBean = getObject();
            O object = seletableBean.getValue();
            if (object != null) {
                oid = object.getOid();
                type = (Class<O>) object.getClass();
            }
        }
    }

    protected PageBase getPageBase() {
        throw new UnsupportedOperationException("Must be implemented in caller.");
    }

}
