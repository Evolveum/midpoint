/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistinctSearchOptionType;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;

/**
 * @author lazyman
 */
public abstract class BaseSearchDataProvider<C extends Containerable, T extends Serializable> extends BaseSortableDataProvider<T> {

    private IModel<Search<C>> search;
    private Class<C> oldType;
    private Map<String, Object> variables = new HashMap<>();

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search) {
        this(component, search, false, true);
    }

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search, boolean useCache) {
        this(component, search, useCache, true);
    }

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search, boolean useCache, boolean useDefaultSortingField) {
        super(component, useCache, useDefaultSortingField);
        this.search = search;
        this.oldType = search.getObject() == null ? null : search.getObject().getTypeClass();
    }

    @Override
    public ObjectQuery getQuery() {
        ExpressionVariables expVariables = new ExpressionVariables();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() == null) {
                expVariables.put(entry.getKey(), null, Object.class);
            } else {
                expVariables.put(entry.getKey(), entry.getValue(), entry.getValue().getClass());
            }
        }
        return search.getObject() == null ? null : search.getObject().createObjectQuery(expVariables.isEmpty() ? null : expVariables, getPageBase(), getCustomizeContentQuery());
    }

    protected ObjectQuery getCustomizeContentQuery() {
        return null;
    }

    public Class<C> getType(){
        return search.getObject() == null ? null : search.getObject().getTypeClass();
    }

    @Override
    public long size() {
        if (search.getObject() != null && !search.getObject().getTypeClass().equals(oldType) && isUseCache()) {
            clearCache();
            oldType = search.getObject().getTypeClass();
        }
        return super.size();
    }

    //    public void setType(Class<C> type) {
//        this.type = type;
//    }

    public void addQueryVariables(String name, Object value) {
        this.variables.put(name, value);
    }

}
