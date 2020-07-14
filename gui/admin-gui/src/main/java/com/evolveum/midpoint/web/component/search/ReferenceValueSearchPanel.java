/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author honchar
 */
public class ReferenceValueSearchPanel extends BasePanel<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_REFERENCE_VALUE_TEXT_FIELD = "referenceValueTextField";
    private static final String ID_EDIT_BUTTON = "editReferenceButton";
    private static final String ID_REF_POPOVER_PANEL = "refPopoverPanel";
    private static final String ID_REF_POPOVER_BODY = "refPopoverBody";
    private static final String ID_REF_POPOVER = "refPopover";
    private PrismReferenceDefinition referenceDef;

    public ReferenceValueSearchPanel(String id, IModel<ObjectReferenceType> model, PrismReferenceDefinition referenceDef) {
        super(id, model);
        this.referenceDef = referenceDef;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        TextPanel<String> referenceTextValueField = new TextPanel<String>(ID_REFERENCE_VALUE_TEXT_FIELD, getReferenceTextValueModel());
        referenceTextValueField.add(AttributeAppender.append("title", getReferenceTextValueModel()));
        referenceTextValueField.setOutputMarkupId(true);
        referenceTextValueField.setEnabled(false);
        add(referenceTextValueField);

        AjaxButton editButton = new AjaxButton(ID_EDIT_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                togglePopover(target, ReferenceValueSearchPanel.this.get(ID_REFERENCE_VALUE_TEXT_FIELD),
                        ReferenceValueSearchPanel.this.get(createComponentPath(ID_REF_POPOVER)), 0);
            }
        };
        editButton.setOutputMarkupId(true);
        add(editButton);

        WebMarkupContainer popover = new WebMarkupContainer(ID_REF_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer popoverBody = new WebMarkupContainer(ID_REF_POPOVER_BODY);
        popoverBody.setOutputMarkupId(true);
        popover.add(popoverBody);

        ReferenceValueSearchPopupPanel value =
                new ReferenceValueSearchPopupPanel(ID_REF_POPOVER_PANEL, getModel()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<QName> getAllowedRelations() {
                        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, getPageBase());
                    }

                    @Override
                    protected List<QName> getSupportedTargetList() {
                            return WebComponentUtil.createSupportedTargetTypeList((referenceDef).getTargetTypeName());
                    }

                    @Override
                    protected void confirmPerformed(AjaxRequestTarget target) {
                        target.add(ReferenceValueSearchPanel.this);
                        referenceValueUpdated(ReferenceValueSearchPanel.this.getModelObject());
                    }
                };
        value.setRenderBodyOnly(true);
        popoverBody.add(value);

    }

    private LoadableModel<String> getReferenceTextValueModel(){
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                return WebComponentUtil.getReferenceObjectTextValue(getModelObject(), getPageBase());
            }
        };
    }

    protected void referenceValueUpdated(ObjectReferenceType ort){
    }

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        StringBuilder script = new StringBuilder();
        script.append("toggleSearchPopover('");
        script.append(button.getMarkupId()).append("','");
        script.append(popover.getMarkupId()).append("',");
        script.append(paddingRight).append(");");

        target.appendJavaScript(script.toString());
    }

}
