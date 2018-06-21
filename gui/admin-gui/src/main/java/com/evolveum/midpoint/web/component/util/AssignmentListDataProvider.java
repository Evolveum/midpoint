/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author katkav
 */
public class AssignmentListDataProvider extends BaseSortableDataProvider<ContainerValueWrapper<AssignmentType>> {

    private IModel<List<ContainerValueWrapper<AssignmentType>>> model;

	private boolean sortable;			// just to ensure backward compatibility with existing usages

    public AssignmentListDataProvider(Component component, IModel<List<ContainerValueWrapper<AssignmentType>>> model) {
		this(component, model, false);
	}

    public AssignmentListDataProvider(Component component, IModel<List<ContainerValueWrapper<AssignmentType>>> model, boolean sortable) {
        super(component);

        Validate.notNull(model);
        this.model = model;
		this.sortable = sortable;
    }


    @Override
    public Iterator<? extends ContainerValueWrapper<AssignmentType>> internalIterator(long first, long count) {
        getAvailableData().clear();

        List<ContainerValueWrapper<AssignmentType>> list = searchThroughList();

		if (sortable && getSort() != null) {
			sort(list);
		}
        if (list != null) {
            for (long i = first; i < first + count; i++) {
                if (i < 0 || i >= list.size()) {
                    throw new ArrayIndexOutOfBoundsException("Trying to get item on index " + i
                            + " but list size is " + list.size());
                }
                getAvailableData().add(list.get(WebComponentUtil.safeLongToInteger(i)));
            }
        }

        return getAvailableData().iterator();
    }

    @SuppressWarnings("unchecked")
	protected <V extends Comparable<V>> void sort(List<ContainerValueWrapper<AssignmentType>> list) {
		Collections.sort(list, new Comparator<ContainerValueWrapper<AssignmentType>>() {
			@Override
			public int compare(ContainerValueWrapper<AssignmentType> o1, ContainerValueWrapper<AssignmentType> o2) {
				SortParam<String> sortParam = getSort();
				String propertyName = sortParam.getProperty();
				V prop1, prop2;
				try {
					prop1 = (V) PropertyUtils.getProperty(o1.getContainerValue().asContainerable(), propertyName);
					prop2 = (V) PropertyUtils.getProperty(o2.getContainerValue().asContainerable(), propertyName);
				} catch (RuntimeException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
					throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
				}
				int comparison = ObjectUtils.compare(prop1, prop2, true);
				return sortParam.isAscending() ? comparison : -comparison;
			}
		});
	}

    @Override
    protected int internalSize() {
        List<ContainerValueWrapper<AssignmentType>> list = searchThroughList();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

    public List<ContainerValueWrapper<AssignmentType>> getSelectedData() {
    	return getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
    }

    protected List<ContainerValueWrapper<AssignmentType>> searchThroughList() {
    	List<ContainerValueWrapper<AssignmentType>> list = model.getObject();

    	if (list == null || list.isEmpty()) {
    		return null;
    	}

    	if (getQuery() == null || getQuery().getFilter() == null) {
    		return list;
    	}

    	List<ContainerValueWrapper<AssignmentType>> filtered = list.stream().filter(a -> {
			try {
				return ObjectQuery.match(a.getContainerValue().asContainerable(), getQuery().getFilter(), null);
			} catch (SchemaException e) {
				throw new TunnelException(e.getMessage());
			}
		}).collect(Collectors.toList());
    	return filtered;

    }




}
