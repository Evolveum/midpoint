/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

//	public boolean isDisplayed(String name) {
//		for (ObjectClassDto objectClass : getFilteredClasses()) {
//			if (objectClass.getDisplayName().equals(name)) {
//				return true;
//			}
//		}
//		return false;
//	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getFilter() {
		return filter;
	}
}
