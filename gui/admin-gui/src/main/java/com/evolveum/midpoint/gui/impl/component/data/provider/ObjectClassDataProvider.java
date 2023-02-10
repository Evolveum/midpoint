/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.web.component.util.SelectableBean;

/**
 * @author lskublik
 */
public class ObjectClassDataProvider extends SelectableListDataProvider<SelectableBean<ObjectClassWrapper>, ObjectClassWrapper> {

    public final static String F_FILTER = "filter";

    private String filter;

    public ObjectClassDataProvider(Component Component, IModel<List<ObjectClassWrapper>> model) {
        super(Component, model);
    }

    public List<ObjectClassWrapper> getListFromModel() {
        if (StringUtils.isEmpty(filter)) {
            return getModel().getObject();
        }
        List<ObjectClassWrapper> rv = new ArrayList<>();
        for (ObjectClassWrapper dto : getModel().getObject()) {
            if (StringUtils.containsIgnoreCase(dto.getObjectClassNameAsString(), filter)
                    || StringUtils.containsIgnoreCase(dto.getNativeObjectClass(), filter)) {
                rv.add(dto);
            }
        }
        return rv;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public long size() {
        return super.size();
    }
}
