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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar.
 */
public class InducementDetailsPanel<R extends AbstractRoleType> extends AbstractRoleAssignmentDetailsPanel<R> {
    private static final long serialVersionUID = 1L;

    public InducementDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(id, form, assignmentModel);
    }

//    @Override
//    protected void initContainersPanel(Form form, PageAdminObjectDetails<R> pageBase){
//        ContainerWrapperListFromObjectWrapperModel<ConstructionType, R> containerModel =
//                new ContainerWrapperListFromObjectWrapperModel<ConstructionType, R>(pageBase.getObjectModel(), collectContainersToShow());
//        if (containerModel != null){
//            containerModel.getObject().forEach(container -> {
//                if (container.getName().equals(AssignmentType.F_CONSTRUCTION) &&
//                        container.getValues().size() > 0){
//                    container.getValues().get(0).getItems().forEach(constructionContainerItem -> {
//                        if (constructionContainerItem instanceof PrismContainer &&
//                                ((PrismContainer) constructionContainerItem).getElementName().equals(ConstructionType.F_ASSOCIATION)){
//                            PrismContainer<ResourceObjectAssociationType> associationCont =
//                                    (PrismContainer<ResourceObjectAssociationType>)constructionContainerItem;
//                            ((PrismContainer) constructionContainerItem).getValue().
//                        }
//                    });
//
//
//
//                    List<ResourceObjectAssociationType> associations = construction.getAssociation();
//                    if (associations != null){
//                        associations.forEach(association -> {
//
//                        });
//                    }
//                    ContainerValueWrapper associationContainer =
//                            container.findContainerValueWrapper(container.getPath().append(ConstructionType.F_ASSOCIATION));
//                    if (associationContainer != null && associationContainer.getItems() != null) {
//                        associationContainer.getItems().forEach(itemWrapper -> {
//                            if (itemWrapper instanceof ContainerWrapper && ((ContainerWrapper) itemWrapper).getPath().last().toString()
//                                    .equals(ResourceObjectAssociationType.F_OUTBOUND.getLocalPart())) {
//                                ((ContainerWrapper) itemWrapper).setAddContainAssignmentTypeerButtonVisible(true);
//                                ((ContainerWrapper) itemWrapper).setShowEmpty(true, false);
//                            }
//                        });
//                    }
//                }
//            });
//        }

//        PrismPanel<AssignmentType> containers = new PrismPanel<>(ID_SPECIFIC_CONTAINERS, containerModel,
//                null, form, null, pageBase) ;
//        add(containers);
//    }

}
