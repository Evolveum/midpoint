/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PopupObjectListPanel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteReferenceRenderer;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ReferenceValueSearchPopupPanel<O extends ObjectType> extends PopoverSearchPopupPanel<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OID = "oid";
    private static final String ID_NAME = "name";
    private static final String ID_TYPE = "type";
    private static final String ID_RELATION = "relation";
    private static final String ID_SELECT_OBJECT_BUTTON = "selectObject";
    private static final String ID_FEEDBACK = "feedback";

    public ReferenceValueSearchPopupPanel(String id, IModel<ObjectReferenceType> model) {
        super(id, model);
    }

    @Override
    protected void customizationPopoverForm(MidpointForm midpointForm) {
        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        midpointForm.add(feedback);

        PropertyModel<String> oidModel = new PropertyModel<>(getModel(), "oid"){
            @Override
            public void setObject(String object) {
                super.setObject(object);
                if (StringUtils.isBlank(object)) {
                    ReferenceValueSearchPopupPanel.this.getModelObject().asReferenceValue().setObject(null);
                    ReferenceValueSearchPopupPanel.this.getModelObject().setTargetName(null);
                    ReferenceValueSearchPopupPanel.this.getModelObject().setRelation(null);
                }
            }
        };
        TextField<String> oidField = new TextField<>(ID_OID, oidModel);
        oidField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        oidField.setOutputMarkupId(true);
        oidField.add(new EnableBehaviour(() -> isItemPanelEnabled()));
        midpointForm.add(oidField);

        ReferenceAutocomplete nameField = new ReferenceAutocomplete(ID_NAME, Model.of(getModelObject()),
                new AutoCompleteReferenceRenderer(),
                getPageBase()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Class<O> getReferenceTargetObjectType() {
                if (getModelObject().getType() == null) {
                    return (Class<O>) ObjectType.class;
                }
                return (Class<O>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), getModelObject().getType());
            }

            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return ReferenceValueSearchPopupPanel.this.isAllowedNotFoundObjectRef();
            }
        };

        feedback.setFilter(new ComponentFeedbackMessageFilter(nameField));
        nameField.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(midpointForm.get(ID_FEEDBACK));

                ObjectReferenceType ort = nameField.getBaseFormComponent().getModelObject();
                if (ort == null) {
                    return;
                }
                ReferenceValueSearchPopupPanel.this.getModel().setObject(ort);
                target.add(midpointForm.get(ID_OID));
            }
        });
        nameField.setOutputMarkupId(true);
        nameField.add(new EnableBehaviour(() -> isItemPanelEnabled()));
        midpointForm.add(nameField);

        DropDownChoicePanel<QName> type = new DropDownChoicePanel<QName>(ID_TYPE, new PropertyModel<>(getModel(), "type"),
                Model.ofList(getSupportedTargetList()), new QNameObjectTypeChoiceRenderer(), true);
        type.setOutputMarkupId(true);
        type.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getSupportedTargetList().size() > 1 && isItemPanelEnabled();
            }
        });
        type.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        midpointForm.add(type);

        List<QName> allowedRelations = new ArrayList<>(getAllowedRelations());
        DropDownChoicePanel<QName> relation = new DropDownChoicePanel<QName>(ID_RELATION,
                new PropertyModel<>(getModel(), "relation"),
                Model.ofList(allowedRelations), new QNameObjectTypeChoiceRenderer(), true);
        relation.setOutputMarkupId(true);
        relation.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getAllowedRelations().size() > 1 && isItemPanelEnabled();
            }
        });
        relation.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        midpointForm.add(relation);

        AjaxButton selectObject = new AjaxButton(ID_SELECT_OBJECT_BUTTON,
                createStringResource("ReferenceValueSearchPopupPanel.selectObject")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<QName> supportedTypes = getSupportedTargetList();
                if (CollectionUtils.isEmpty(supportedTypes)) {
                    supportedTypes = WebComponentUtil.createObjectTypeList();
                }
                ObjectBrowserPanel<O> objectBrowserPanel = new ObjectBrowserPanel<O>(
                        getPageBase().getMainPopupBodyId(), null, supportedTypes, false, getPageBase(),
                        null) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectPerformed(AjaxRequestTarget target, O object) {
                        getPageBase().hideMainPopup(target);
                        if (ReferenceValueSearchPopupPanel.this.getModel().getObject() == null) {
                            ReferenceValueSearchPopupPanel.this.getModel().setObject(new ObjectReferenceType());
                        }
                        ReferenceValueSearchPopupPanel.this.getModelObject().setOid(object.getOid());
                        ReferenceValueSearchPopupPanel.this.getModelObject().setTargetName(object.getName());
                        ReferenceValueSearchPopupPanel.this.getModelObject().setType(
                                object.asPrismObject().getComplexTypeDefinition().getTypeName());
                        target.add(oidField);
                        target.add(nameField);
                        target.add(type);
                    }
                };

                getPageBase().showMainPopup(objectBrowserPanel, target);
            }
        };
        midpointForm.add(selectObject);
    }

    protected List<QName> getAllowedRelations() {
        return WebComponentUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getSupportedTargetList() {
        return WebComponentUtil.createFocusTypeList();
    }

    protected boolean isAllowedNotFoundObjectRef(){
        return false;
    }

}
