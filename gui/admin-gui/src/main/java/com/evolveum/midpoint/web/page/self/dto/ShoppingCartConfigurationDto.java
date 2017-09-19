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
package com.evolveum.midpoint.web.page.self.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionUseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class ShoppingCartConfigurationDto implements Serializable {
    private static final long serialVersionUID = 1L;

    AssignmentViewType defaultViewType;
    List<AssignmentViewType> viewTypeList;
    String roleCatalogOid = null;
    AssignmentConstraintsType defaultAssignmentConstraints;

    public void initShoppingCartConfigurationDto(RoleManagementConfigurationType roleManagementConfiguration) {
        if (roleManagementConfiguration == null){
            viewTypeList = new ArrayList<>(Arrays.asList(AssignmentViewType.values()));
            defaultViewType = AssignmentViewType.ROLE_TYPE;
            return;
        }

        computeViewTypeList(roleManagementConfiguration);
        initRoleCatalogOid(roleManagementConfiguration);
        computeDefaultViewType(roleManagementConfiguration);

        this.defaultAssignmentConstraints = roleManagementConfiguration.getDefaultAssignmentConstraints();
    }

    private List<AssignmentViewType> getRoleCatalogViewsList(RoleManagementConfigurationType roleManagementConfiguration) {
        List<AssignmentViewType> assignmentViewTypes= new ArrayList<>();
        if (isRoleCatalogCollectionsListEmpty(roleManagementConfiguration)){
            return new ArrayList<>(Arrays.asList(AssignmentViewType.values()));
        }
        for (ObjectCollectionUseType collection :
                roleManagementConfiguration.getRoleCatalogCollections().getCollection()){
            AssignmentViewType viewType = initViewTypeValue(collection);
            if (viewType != null){
                assignmentViewTypes.add(viewType);
            }
        }

        return assignmentViewTypes;
    }

    private boolean isRoleCatalogCollectionsListEmpty(RoleManagementConfigurationType roleManagementConfiguration){
        return roleManagementConfiguration.getRoleCatalogCollections() == null ||
                roleManagementConfiguration.getRoleCatalogCollections().getCollection() == null ||
                roleManagementConfiguration.getRoleCatalogCollections().getCollection().size() == 0;
    }

    public AssignmentViewType initViewTypeValue(ObjectCollectionUseType collectionType){
        if (collectionType == null){
            return null;
        }
        for (AssignmentViewType viewType : AssignmentViewType.values()){
            if (viewType.getUri().equals(collectionType.getCollectionUri())){
                return viewType;
            }
        }
        return null;
    }

    private void computeViewTypeList(RoleManagementConfigurationType roleManagementConfiguration){
        viewTypeList = getRoleCatalogViewsList(roleManagementConfiguration);
        if (viewTypeList.isEmpty()){
            //use default views list if nothing is configured
            viewTypeList = new ArrayList<>(Arrays.asList(AssignmentViewType.values()));
        }
    }

    private void initRoleCatalogOid(RoleManagementConfigurationType roleManagementConfiguration){
        if (roleManagementConfiguration.getRoleCatalogRef() == null){
            return;
        }
        roleCatalogOid = roleManagementConfiguration.getRoleCatalogRef().getOid();
    }

    private void computeDefaultViewType(RoleManagementConfigurationType roleManagementConfiguration){
        defaultViewType = initViewTypeValue(roleManagementConfiguration.getDefaultCollection());
        if (defaultViewType != null && !viewTypeList.contains(defaultViewType)){
            viewTypeList.add(defaultViewType);
        }
        if (defaultViewType == null){
            if (viewTypeList.size() == 1){
                defaultViewType = viewTypeList.get(0);
                return;
            }
            if (StringUtils.isNotEmpty(roleCatalogOid) && viewTypeList.contains(AssignmentViewType.ROLE_CATALOG_VIEW)) {
                defaultViewType = AssignmentViewType.ROLE_CATALOG_VIEW;
                return;
            }
            if (StringUtils.isEmpty(roleCatalogOid) && AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeList.get(0))) {
                defaultViewType = viewTypeList.get(1);
                return;
            }
            defaultViewType = viewTypeList.get(0);
        }
    }

    public AssignmentViewType getDefaultViewType() {
        return defaultViewType;
    }

    public void setDefaultViewType(AssignmentViewType defaultViewType) {
        this.defaultViewType = defaultViewType;
    }

    public List<AssignmentViewType> getViewTypeList() {
        return viewTypeList;
    }

    public void setViewTypeList(List<AssignmentViewType> viewTypeList) {
        this.viewTypeList = viewTypeList;
    }

    public String getRoleCatalogOid() {
        return roleCatalogOid;
    }

    public void setRoleCatalogOid(String roleCatalogOid) {
        this.roleCatalogOid = roleCatalogOid;
    }

    public AssignmentConstraintsType getDefaultAssignmentConstraints() {
        return defaultAssignmentConstraints;
    }

    public void setDefaultAssignmentConstraints(AssignmentConstraintsType defaultAssignmentConstraints) {
        this.defaultAssignmentConstraints = defaultAssignmentConstraints;
    }
}
