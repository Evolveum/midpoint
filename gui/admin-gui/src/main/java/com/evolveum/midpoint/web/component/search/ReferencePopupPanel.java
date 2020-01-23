/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ReferencePopupPanel extends SearchPopupPanel<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OID = "oid";
    private static final String ID_TYPE = "type";
    private static final String ID_RELATION = "relation";

//    private List<QName> allowedRelations;

    public ReferencePopupPanel(String id, IModel<DisplayableValue<ObjectReferenceType>> model) {
        super(id, model);
//        this.allowedRelations = allowedRelations;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        TextField<String> oidField = new TextField<String>(ID_OID, new PropertyModel<>(getModel(), SearchValue.F_VALUE + ".oid"));

        oidField.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }
        });
        oidField.setOutputMarkupId(true);
        add(oidField);

        DropDownChoice<QName> type = new DropDownChoice<>(ID_TYPE, new PropertyModel<QName>(getModel(), SearchValue.F_VALUE + ".type"),
                getSupportedTargetList(), new QNameObjectTypeChoiceRenderer());
        type.setNullValid(true);
        type.setOutputMarkupId(true);
        type.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getSupportedTargetList().size() > 1;
            }
        });

        add(type);

        if (getModelObject().getValue() != null && getModelObject().getValue().getRelation() == null){
            getModelObject().getValue().setRelation(PrismConstants.Q_ANY);
        }
        List<QName> allowedRelations = new ArrayList<QName>();
        allowedRelations.addAll(getAllowedRelations());
        if (!allowedRelations.contains(PrismConstants.Q_ANY)) {
            allowedRelations.add(0, PrismConstants.Q_ANY);
        }
        DropDownChoice<QName> relation = new DropDownChoice<>(ID_RELATION, new PropertyModel<QName>(getModel(), SearchValue.F_VALUE + ".relation"),
                allowedRelations, new QNameObjectTypeChoiceRenderer());
//        relation.setNullValid(true);
        relation.setOutputMarkupId(true);
        relation.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getAllowedRelations().size() > 1;
            }
        });
        add(relation);
    }

    protected List<QName> getAllowedRelations() {
        return WebComponentUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getSupportedTargetList() {
        return WebComponentUtil.createFocusTypeList();
    }


}
