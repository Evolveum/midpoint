/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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

    public ReferenceValueSearchPopupPanel(String id, IModel<ObjectReferenceType> model) {
        super(id, model);
    }

    @Override
    protected void customizationPopoverForm(MidpointForm midpointForm) {
        TextField<String> oidField = new TextField<>(ID_OID, new PropertyModel<>(getModel(), "oid"));
        oidField.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }
        });
        oidField.setOutputMarkupId(true);
        oidField.add(new VisibleBehaviour(() -> true));
        oidField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                ReferenceValueSearchPopupPanel.this.getModelObject().asReferenceValue().setObject(null);
                ReferenceValueSearchPopupPanel.this.getModelObject().setTargetName(null);
                ReferenceValueSearchPopupPanel.this.getModelObject().setRelation(null);
            }
        });
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
        };

        nameField.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ObjectReferenceType ort = nameField.getBaseFormComponent().getModelObject();
                if (ort == null) {
                    return;
                }
                ReferenceValueSearchPopupPanel.this.getModel().setObject(ort);
                target.add(midpointForm.get(ID_OID));
            }
        });
        nameField.setOutputMarkupId(true);
        midpointForm.add(nameField);

        DropDownChoicePanel<QName> type = new DropDownChoicePanel<QName>(ID_TYPE, new PropertyModel<>(getModel(), "type"),
                Model.ofList(getSupportedTargetList()), new QNameObjectTypeChoiceRenderer(), true);
        type.setOutputMarkupId(true);
        type.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getSupportedTargetList().size() > 1;
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
                return getAllowedRelations().size() > 1;
            }
        });
        relation.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        midpointForm.add(relation);
    }

    protected List<QName> getAllowedRelations() {
        return WebComponentUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getSupportedTargetList() {
        return WebComponentUtil.createFocusTypeList();
    }

}
