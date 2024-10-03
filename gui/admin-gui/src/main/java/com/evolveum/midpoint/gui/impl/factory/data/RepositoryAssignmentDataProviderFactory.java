/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.data;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.gui.api.factory.ContainerValueDataProviderFactory;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.RepoAssignmentListProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryAssignmentDataProviderType;

import javax.xml.namespace.QName;

@Component
public class RepositoryAssignmentDataProviderFactory  implements ContainerValueDataProviderFactory<AssignmentType, RepositoryAssignmentDataProviderType>{

    @Override
    public Class<AssignmentType> getDataType() {
        return AssignmentType.class;
    }

    @Override
    public Class<RepositoryAssignmentDataProviderType> getConfigurationType() {
        return RepositoryAssignmentDataProviderType.class;
    }

    @Override
    public boolean isRepositorySearchEnabled() {
        return true;
    }

    @Override
    public ISelectableDataProvider<PrismContainerValueWrapper<AssignmentType>> create(
        org.apache.wicket.Component component, @NotNull IModel<Search<AssignmentType>> search,
        IModel<List<PrismContainerValueWrapper<AssignmentType>>> model, Class<? extends Objectable> objectType,
        String oid, ItemPath path) {
        return new RepoAssignmentListProvider(component, search, model, objectType, oid, path);
    }

    @Override
    public ISelectableDataProvider<PrismContainerValueWrapper<AssignmentType>> create(
        org.apache.wicket.Component component, @NotNull IModel<Search<AssignmentType>> search,
        IModel<List<PrismContainerValueWrapper<AssignmentType>>> model, Class<? extends Objectable> objectType,
        String oid, ItemPath path, CompiledObjectCollectionView collection, Customization<AssignmentType> customization) {
        return doCreate(component, search, model, objectType, oid, path, collection, customization);
    }

    private static ISelectableDataProvider<PrismContainerValueWrapper<AssignmentType>> doCreate(
        org.apache.wicket.Component component, @NotNull IModel<Search<AssignmentType>> search,
        IModel<List<PrismContainerValueWrapper<AssignmentType>>> model, Class<? extends Objectable> objectType,
        String oid, ItemPath path, CompiledObjectCollectionView collection, Customization<AssignmentType> customization) {
        RepoAssignmentListProvider provider = new RepoAssignmentListProvider(component, search, model, objectType, oid, path) {

            @Override
            protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(
                    List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
                return customization.postFilter(assignmentList);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return customization.getCustomizeContentQuery();
            }

            @Override
            protected QName determineTargetRefType() {
                var targetRefDef = customization.getDefinition().findReferenceDefinition(AssignmentType.F_TARGET_REF);
                QName targetType = targetRefDef.getTargetTypeName();
                if (targetType != null && !Objects.equals(AssignmentHolderType.COMPLEX_TYPE, targetType)) {
                    // target type was overriden
                    return targetType;
                }
                return null;
            }
        };
        provider.setCompiledObjectCollectionView(collection);
        return provider;
    }

}
