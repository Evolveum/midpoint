/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * Created by honchar.
 */
public abstract class MultiTypesMemberPopupTabPanel<O extends ObjectType> extends MemberPopupTabPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MultiTypesMemberPopupTabPanel.class);

    private static final String ID_TYPE_SELECT_PANEL = "typeSelectPanel";
    private static final String ID_TYPE = "type";

    private ObjectTypes defaultObjectType = ObjectTypes.OBJECT_COLLECTION;

    public MultiTypesMemberPopupTabPanel(String id, Search search, List<ObjectReferenceType> archetypeReferenceList){
        super(id, search, archetypeReferenceList);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        WebMarkupContainer typePanel = new WebMarkupContainer(ID_TYPE_SELECT_PANEL);
        typePanel.setOutputMarkupId(true);
        add(typePanel);

        DropDownChoicePanel<ObjectTypes> typeSelect = new DropDownChoicePanel<ObjectTypes>(ID_TYPE, new LoadableModel<ObjectTypes>() {
            @Override
            protected ObjectTypes load() {
                return defaultObjectType;
            }
        }, Model.ofList(getSupportedTypesList()), new EnumChoiceRenderer<>(this));
        typeSelect.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                defaultObjectType = typeSelect.getModel().getObject();
                MultiTypesMemberPopupTabPanel.this.addOrReplace(initObjectListPanel());
                target.add(MultiTypesMemberPopupTabPanel.this);
            }
        });
        typePanel.add(typeSelect);

        add(typePanel);
    }

    @Override
    protected ObjectDelta prepareDelta(){
        Class classType = WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), getObjectType().getTypeQName());
        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getAbstractRoleTypeObject(), getRelationValue());
        return getPageBase().getPrismContext().deltaFactory().object().createModificationAddReference(classType, "fakeOid",
                ObjectType.F_PARENT_ORG_REF,
                ref.asReferenceValue());
    }

    protected List<ObjectTypes> getSupportedTypesList(){
        List<ObjectTypes> supportedTypes = ObjectTypeListUtil.createAssignmentHolderTypesList();
        supportedTypes.remove(ObjectTypes.USER);
        supportedTypes.remove(ObjectTypes.ROLE);
        supportedTypes.remove(ObjectTypes.SERVICE);
        supportedTypes.remove(ObjectTypes.ORG);
        supportedTypes.remove(ObjectTypes.FOCUS_TYPE);
        supportedTypes.remove(ObjectTypes.ABSTRACT_ROLE);
        supportedTypes.remove(ObjectTypes.NODE);
        supportedTypes.remove(ObjectTypes.SHADOW);

        return supportedTypes;
    }

    @Override
    protected ObjectTypes getObjectType(){
        return defaultObjectType;
    }
}
