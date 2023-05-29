/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.NonEmptyModel;

/**
 * @author lazyman
 */
public class ObjectClassDataProvider implements IDataProvider<ObjectClassDto> {

    @NotNull private final NonEmptyModel<List<ObjectClassDto>> allClasses;
    private String filter;

    public ObjectClassDataProvider(@NotNull NonEmptyModel<List<ObjectClassDto>> allClasses) {
        this.allClasses = allClasses;
    }

    @Override
    public Iterator<? extends ObjectClassDto> iterator(long first, long count) {
        List<ObjectClassDto> data = new ArrayList<>();
        List<ObjectClassDto> filteredClasses = getFilteredClasses();
        for (int i = (int) first; i < filteredClasses.size() && i < first + count; i++) {
            data.add(filteredClasses.get(i));
        }
        return data.iterator();
    }

    @Override
    public long size() {
        return getFilteredClasses().size();
    }

    @Override
    public IModel<ObjectClassDto> model(ObjectClassDto object) {
        return new Model<>(object);
    }

    // not cached because of data staleness issues (when source model(s) get reset)
    private List<ObjectClassDto> getFilteredClasses() {
        if (StringUtils.isEmpty(filter)) {
            return allClasses.getObject();
        }
        List<ObjectClassDto> rv = new ArrayList<>();
        for (ObjectClassDto dto : allClasses.getObject()) {
            if (StringUtils.containsIgnoreCase(dto.getDisplayName(), filter)) {
                rv.add(dto);
            }
        }
        return rv;
    }

    @Override
    public void detach() {
    }

//    public boolean isDisplayed(String name) {
//        for (ObjectClassDto objectClass : getFilteredClasses()) {
//            if (objectClass.getDisplayName().equals(name)) {
//                return true;
//            }
//        }
//        return false;
//    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }
}
