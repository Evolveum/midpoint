/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;

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
    private AvailableRelationDto supportedRelationList = new AvailableRelationDto();

    public MemberPopupTabPanel(String id, AvailableRelationDto supportedRelationList){
        super(id);
        this.supportedRelationList = supportedRelationList;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        pageBase = getPageBase();
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel){
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        relationContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return CollectionUtils.isNotEmpty(supportedRelationList.getAvailableRelationList());
            }

            @Override
            public boolean isEnabled(){
                return CollectionUtils.isNotEmpty(supportedRelationList.getAvailableRelationList())
                        && supportedRelationList.getAvailableRelationList().size() > 1;
            }
        });
        parametersPanel.add(relationContainer);

        relationContainer.add(new RelationDropDownChoicePanel(ID_RELATION, supportedRelationList.getDefaultRelation(), supportedRelationList.getAvailableRelationList(), false));
    }

    protected ObjectDelta prepareDelta(){
        ObjectDelta delta = null;
        try {
            Class classType = WebComponentUtil.qnameToClass(pageBase.getPrismContext(), getObjectType().getTypeQName());
            delta =  pageBase.getPrismContext().deltaFactory().object().createEmptyModifyDelta(classType, "fakeOid"
            );
            AssignmentType newAssignment = new AssignmentType();
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getAbstractRoleTypeObject(), getRelationValue());
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
        RelationDropDownChoicePanel relationPanel = getRelationDropDown();
        return relationPanel.getRelationValue();
    }

    private RelationDropDownChoicePanel getRelationDropDown(){
        return (RelationDropDownChoicePanel)get(ID_PARAMETERS_PANEL).get(ID_RELATION_CONTAINER).get(ID_RELATION);
    }
}
