/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.*;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class AbstractObjectListPanel<O extends ObjectType> extends MainObjectListPanel<O> {

    private final ObjectDetailsModels<O> objectDetailsModel;

    public AbstractObjectListPanel(String id, ObjectDetailsModels<O> model, ContainerPanelConfigurationType config) {
        super(id, (Class<O>) ObjectType.class, config);
        objectDetailsModel = model;
    }

    public ObjectDetailsModels<O> getObjectDetailsModel() {
        return objectDetailsModel;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return false;
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<O>> createProvider() {
        return createSelectableBeanObjectDataProvider(() -> getCustomizeContentQuery(), null);
    }

    protected ObjectQuery getCustomizeContentQuery(){
        return null;
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();
        ctx.setPanelType(CollectionPanelType.ASSIGNABLE);
        return ctx;
    }

    @Override
    protected String getStorageKey() {
        String suffix = getTableId().name();
        if (getPanelConfiguration() != null) {
            suffix = getPanelConfiguration().getIdentifier();
        }
        return WebComponentUtil.getObjectListPageStorageKey(suffix);
    }
}
