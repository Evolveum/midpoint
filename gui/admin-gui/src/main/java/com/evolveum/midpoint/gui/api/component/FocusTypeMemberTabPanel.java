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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class FocusTypeMemberTabPanel<F extends FocusType> extends AbstractPopupTabPanel<F>{
    private static final long serialVersionUID = 1L;

    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    public FocusTypeMemberTabPanel(String id, ObjectTypes type){
        super(id, type);
    }

    @Override
    protected void initParametersPanel(){
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        add(relationContainer);

        DropDownChoicePanel<RelationTypes> relationSelector = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), Model.of(RelationTypes.MEMBER),
                FocusTypeMemberTabPanel.this, false);
        relationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
        relationContainer.add(relationSelector);
    }

    protected ObjectDelta getMemberDelta(){
        ObjectDelta delta = null;
        try {
            Class classType = WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), type.getTypeQName());
            delta =  ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", getPageBase().getPrismContext());
            AssignmentType newAssignment = new AssignmentType();
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject());
            ref.setRelation(getRelationValue());
            newAssignment.setTargetRef(ref);

            getPageBase().getPrismContext().adopt(newAssignment);
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, newAssignment);

        } catch (SchemaException e) {
            //TODO
        }

        return delta;
    }

    public QName getRelationValue(){
        DropDownChoicePanel<RelationTypes> relationPanel = getRelationDropDown();
        RelationTypes relation = relationPanel.getModel().getObject();
        if (relation == null) {
            return SchemaConstants.ORG_DEFAULT;
        }
        return relation.getRelation();
    }

    private DropDownChoicePanel getRelationDropDown(){
        return (DropDownChoicePanel)get(ID_RELATION_CONTAINER).get(ID_RELATION);
    }

    protected ObjectQuery createInOidMemberQuery(){
        List<F> selectedObjects = getSelectedObjectsList();
        List<String> oids = new ArrayList<>();
        selectedObjects.forEach(selectedObject -> {
            oids.add(selectedObject.getOid());
        });
        return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
    }
}
