/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractObjectListPanel<O extends ObjectType> extends MainObjectListPanel<O> {

    private final ObjectDetailsModels<O> objectDetailsModel;

    public AbstractObjectListPanel(String id, ObjectDetailsModels<O> model, ContainerPanelConfigurationType config) {
        super(id, (Class<O>) ObjectType.class, null, config);
        objectDetailsModel = model;
    }

    public ObjectDetailsModels<O> getObjectDetailsModel() {
        return objectDetailsModel;
    }

    @Override
    protected boolean isCreateNewObjectEnabled() {
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
    protected Search createSearch(Class<O> type) {
        Search search = SearchFactory.createMemberSearch(type, getSupportedTypes(), getSupportedRelations(), getAbstractRoleType(),  getObjectCollectionView(), getPageBase());
        search.getAllowedTypeList()
                .addAll(Arrays.asList(
                        AbstractRoleType.class,
                        OrgType.class,
                        ArchetypeType.class,
                        RoleType.class,
                        ServiceType.class));
        return search;
    }

    protected List<QName> getSupportedTypes() {
        return Arrays.asList(
                AbstractRoleType.COMPLEX_TYPE,
                OrgType.COMPLEX_TYPE,
                ArchetypeType.COMPLEX_TYPE,
                RoleType.COMPLEX_TYPE,
                ServiceType.COMPLEX_TYPE);
    }

    protected List<QName> getSupportedRelations() {
        return Collections.emptyList();
    }

    protected QName getAbstractRoleType() {
        return getObjectDetailsModel().getObjectWrapper().getTypeName();
    }

//    protected SearchConfigurationWrapper<O> createSearchBoxConfigurationWrapper() {
//        CompiledObjectCollectionView view = getObjectCollectionView();
//
//        SearchConfigurationWrapper<O> searchWrapper;
//        if (getPanelConfiguration() != null
//                && getPanelConfiguration().getListView() != null
//                && getPanelConfiguration().getListView().getSearchBoxConfiguration() != null) {
//            searchWrapper = new SearchConfigurationWrapper<>(getPanelConfiguration().getListView().getSearchBoxConfiguration());
//        } else if (view != null && view.getSearchBoxConfiguration() != null) {
//            searchWrapper = new SearchConfigurationWrapper<>(view.getSearchBoxConfiguration());
//        } else {
//            searchWrapper = new SearchConfigurationWrapper<>();
//        }

//        if (view != null
//                && view.getCollection() != null
//                && view.getCollection().getCollectionRef() != null
//                && QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, view.getCollection().getCollectionRef().getType())) {
//            searchWrapper.setCollectionRefOid(view.getCollection().getCollectionRef().getOid());
//        }

//        return searchWrapper;
//    }

    @Override
    protected String getStorageKey() {
        String suffix = getTableId().name();
        if (getPanelConfiguration() != null) {
            suffix = getPanelConfiguration().getIdentifier();
        }
        return WebComponentUtil.getObjectListPageStorageKey(suffix);
    }
}
