/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by honchar.
 */
public class InducedEntitlementDetailsPanel<R extends AbstractRoleType> extends InducementDetailsPanel<R> {
    private static final long serialVersionUID = 1L;

    public InducedEntitlementDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(id, form, assignmentModel);
    }

    @Override
    protected void initContainersPanel(Form form, PageAdminObjectDetails<R> pageBase) {
        ContainerWrapper<ConstructionType> constructionContainer = getModelObject().findContainerWrapper(getModelObject().getPath().append((AssignmentType.F_CONSTRUCTION)));
        ConstructionAssociationPanel constructionDetailsPanel = new ConstructionAssociationPanel(ID_SPECIFIC_CONTAINERS, Model.of(constructionContainer));
        constructionDetailsPanel.setOutputMarkupId(true);
        add(constructionDetailsPanel);
    }

}
