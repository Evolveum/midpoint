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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteReferenceRenderer;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ReferenceValueSearchPopupPanel<O extends ObjectType> extends BasePanel<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OID = "oid";
    private static final String ID_NAME = "name";
    private static final String ID_TYPE = "type";
    private static final String ID_RELATION = "relation";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";

    public ReferenceValueSearchPopupPanel(String id, IModel<ObjectReferenceType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        TextField<String> oidField = new TextField<String>(ID_OID, new PropertyModel<>(getModel(), "oid"));
        oidField.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }
        });
        oidField.setOutputMarkupId(true);
        oidField.add(new VisibleBehaviour(() -> true));
        oidField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(oidField);

        ReferenceAutocomplete nameField = new ReferenceAutocomplete(ID_NAME, Model.of(getModelObject()),
                new AutoCompleteReferenceRenderer(),
                getPageBase()){

            private static final long serialVersionUID = 1L;

            @Override
            protected Class<O> getReferenceTargetObjectType(){
                List<QName> supportedTypes = getSupportedTargetList();
                if (CollectionUtils.isNotEmpty(supportedTypes)){
                    return (Class<O>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), supportedTypes.get(0));
                }
                return (Class<O>) ObjectType.class;
            }

        };

        nameField.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ObjectReferenceType ort = nameField.getBaseFormComponent().getModelObject();
                ReferenceValueSearchPopupPanel.this.getModel().setObject(ort);
                target.add(ReferenceValueSearchPopupPanel.this.get(ID_OID));
            }
        });
        nameField.setOutputMarkupId(true);
        nameField.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(nameField);

        DropDownChoice<QName> type = new DropDownChoice<>(ID_TYPE, new PropertyModel<QName>(getModel(), "type"),
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
        type.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(type);

        if (getModelObject() != null && getModelObject().getRelation() == null){
            getModelObject().setRelation(PrismConstants.Q_ANY);
        }
        List<QName> allowedRelations = new ArrayList<QName>();
        allowedRelations.addAll(getAllowedRelations());
        if (!allowedRelations.contains(PrismConstants.Q_ANY)) {
            allowedRelations.add(0, PrismConstants.Q_ANY);
        }
        DropDownChoice<QName> relation = new DropDownChoice<>(ID_RELATION, new PropertyModel<QName>(getModel(), "relation"),
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
        relation.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(relation);

        AjaxButton confirm = new AjaxButton(ID_CONFIRM_BUTTON, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                confirmPerformed(target);
            }
        };
        add(confirm);

    }

    protected void confirmPerformed(AjaxRequestTarget target){

    }

    protected List<QName> getAllowedRelations() {
        return WebComponentUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getSupportedTargetList() {
        return WebComponentUtil.createFocusTypeList();
    }


}
