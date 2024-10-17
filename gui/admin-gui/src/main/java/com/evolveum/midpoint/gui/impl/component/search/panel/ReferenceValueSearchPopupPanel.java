/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.form.ReferenceAutocompletePanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ReferenceValueSearchPopupPanel extends PopoverSearchPopupPanel<ObjectReferenceType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OID = "oid";
    private static final String ID_NAME = "name";
    private static final String ID_TYPE = "type";
    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";
    private static final String ID_FEEDBACK = "feedback";


    public ReferenceValueSearchPopupPanel(String id, Popover popover, IModel<ObjectReferenceType> model) {
        super(id, popover, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        MidpointForm midpointForm = getPopoverForm();

        ReferenceAutocompletePanel<ObjectReferenceType> nameField = (ReferenceAutocompletePanel) midpointForm.get(ID_NAME);
        nameField.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateModel(nameField.getModelObject(), midpointForm, target);
            }
        });

        nameField.getBaseFormComponent().add(AttributeAppender.append(
                "aria-label",
                createStringResource("ReferencePopupPanel.name")));
    }

    @Override
    protected void customizationPopoverForm(MidpointForm midpointForm) {
        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        midpointForm.add(feedback);

        PropertyModel<String> oidModel = new PropertyModel<>(getModel(), "oid") {
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
        oidField.add(new EnableBehaviour(this::isItemPanelEnabled));
        oidField.add(AttributeAppender.append("aria-label", createStringResource("ReferencePopupPanel.oid")));
        midpointForm.add(oidField);

        ReferenceAutocompletePanel<ObjectReferenceType> nameField = new ReferenceAutocompletePanel<>(ID_NAME, Model.of(getModelObject())) {
            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return ReferenceValueSearchPopupPanel.this.isAllowedNotFoundObjectRef();
            }

            @Override
            protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
                ReferenceValueSearchPopupPanel.this.chooseObjectPerformed(target, object);
            }

            @Override
            public List<QName> getSupportedTypes() {
                ObjectReferenceType ref = ReferenceValueSearchPopupPanel.this.getModelObject();
                if (ref != null && ref.getType() != null) {
                    return Collections.singletonList(ref.getType());
                }
                return Collections.singletonList(ObjectType.COMPLEX_TYPE);
            }
        };

        feedback.setFilter(new ComponentFeedbackMessageFilter(nameField));
        nameField.setOutputMarkupId(true);
        nameField.add(new EnableBehaviour(this::isItemPanelEnabled));
        midpointForm.add(nameField);

        DropDownChoicePanel<QName> type = new DropDownChoicePanel<>(ID_TYPE, new PropertyModel<>(getModel(), "type"),
                Model.ofList(getSupportedTargetList()), new QNameObjectTypeChoiceRenderer(), true);
        type.setOutputMarkupId(true);
        type.add(new VisibleEnableBehaviour() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getSupportedTargetList().size() > 1 && isItemPanelEnabled();
            }
        });
        type.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        type.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ObjectReferenceType ref = nameField.getConverter()
                        .convertToObject(nameField.getBaseFormComponent().getValue(), WebComponentUtil.getCurrentLocale());
                updateModel(ref, midpointForm, target);
                target.add(oidField);
                target.add(ReferenceValueSearchPopupPanel.this);
            }
        });
        type.getBaseFormComponent().add(AttributeAppender.append(
                "aria-label",
                createStringResource("ReferencePopupPanel.targetType")));
        midpointForm.add(type);

        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        midpointForm.add(relationContainer);
        relationContainer.add(new VisibleBehaviour(() -> getAllowedRelations().size() > 1));
        List<QName> allowedRelations = new ArrayList<>(getAllowedRelations());

        DropDownChoicePanel<QName> relation = new DropDownChoicePanel<>(ID_RELATION,
                new PropertyModel<>(getModel(), "relation"),
                Model.ofList(allowedRelations), RelationUtil.getRelationChoicesRenderer(), true);
        relation.setOutputMarkupId(true);
        relation.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        relation.getBaseFormComponent().add(AttributeAppender.append(
                "aria-label",
                createStringResource("ReferencePopupPanel.relation")));
        relationContainer.add(relation);
    }

    private void updateModel(ObjectReferenceType ref, MidpointForm<?> midpointForm, AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());

        if (ref == null) {
            return;
        }
        if (getModelObject() != null && getModelObject().getOid() != null && PolyStringUtils.isEmpty(ref.getTargetName()) && ref.getObject() == null) {
            ref.setOid(getModelObject().getOid());
        }
        if (PolyStringUtils.isEmpty(ref.getTargetName())) {
            ref.setTargetName(null);
        }
        ReferenceValueSearchPopupPanel.this.getModel().setObject(ref);
        target.add(midpointForm.get(ID_OID));
    }

    protected List<QName> getAllowedRelations() {
        return RelationUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getSupportedTargetList() {
        return ObjectTypeListUtil.createFocusTypeList();
    }

    protected boolean isAllowedNotFoundObjectRef() {
        return false;
    }

    protected <O extends ObjectType> void chooseObjectPerformed(AjaxRequestTarget target, O object) {

    }
}
