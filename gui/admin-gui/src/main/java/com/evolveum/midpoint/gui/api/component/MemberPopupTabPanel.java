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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.LimitationsEditorDialog;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class MemberPopupTabPanel<O extends ObjectType> extends AbstractPopupTabPanel<O>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MemberPopupTabPanel.class);

    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    private PageBase pageBase;
    private List<RelationTypes> availableRelationList = new ArrayList<>();

    public MemberPopupTabPanel(String id, ObjectTypes type, List<RelationTypes> availableRelationList){
        super(id, type);
        this.availableRelationList = availableRelationList;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        pageBase = getPageBase();
    }

    @Override
    protected void initParametersPanel(){
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        relationContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return CollectionUtils.isNotEmpty(availableRelationList);
            }

            @Override
            public boolean isEnabled(){
                return CollectionUtils.isNotEmpty(availableRelationList) && availableRelationList.size() > 1;
            }
        });
        add(relationContainer);

        DropDownChoicePanel<RelationTypes> relationSelector =  new DropDownChoicePanel<RelationTypes> (ID_RELATION,
                Model.of(getDefaultRelationValue()), Model.ofList(availableRelationList),
                WebComponentUtil.getEnumChoiceRenderer(MemberPopupTabPanel.this), false);
        relationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
        relationContainer.add(relationSelector);
    }

    private RelationTypes getDefaultRelationValue(){
        return CollectionUtils.isNotEmpty(availableRelationList) ? availableRelationList.get(0) : RelationTypes.MEMBER;
    }

    protected ObjectDelta prepareDelta(){
        ObjectDelta delta = null;
        try {
            Class classType = WebComponentUtil.qnameToClass(pageBase.getPrismContext(), type.getTypeQName());
            delta =  ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", pageBase.getPrismContext());
            AssignmentType newAssignment = new AssignmentType();
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getAbstractRoleTypeObject());
            ref.setRelation(getRelationValue());
            newAssignment.setTargetRef(ref);

            pageBase.getPrismContext().adopt(newAssignment);
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, newAssignment);

        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for adding a member operation ", e);
        }

        return delta;
    }

    protected abstract AbstractRoleType getAbstractRoleTypeObject();

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
}
