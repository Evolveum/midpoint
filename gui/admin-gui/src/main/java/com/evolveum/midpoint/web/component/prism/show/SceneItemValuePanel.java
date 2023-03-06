/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * TODO make this parametric (along with SceneItemValue)
 * @author mederly
 */
public class SceneItemValuePanel extends BasePanel<SceneItemValue> {

    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_LINK = "link";
    private static final String ID_ADDITIONAL_TEXT = "additionalText";

    public SceneItemValuePanel(String id, IModel<SceneItemValue> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        final VisibleEnableBehaviour visibleIfReference = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                SceneItemValue object = getModelObject();
                return hasValidReferenceValue(object);
            }
        };
        final VisibleEnableBehaviour visibleIfNotReference = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                SceneItemValue object = getModelObject();
                return !hasValidReferenceValue(object);
            }
        };

        IModel<DisplayType> displayModel = (IModel) () -> {
            ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor();
            String cssClass = ObjectTypeGuiDescriptor.ERROR_ICON;
            String title = null;
            if (guiDescriptor != null) {
                cssClass = guiDescriptor.getBlackIcon();
                title = createStringResource(guiDescriptor.getLocalizationKey()).getObject();
            }
            return GuiDisplayTypeUtil.createDisplayType(cssClass, "", title);
        };
        final ImagePanel icon = new ImagePanel(ID_ICON, displayModel);
        icon.add(visibleIfReference);
        add(icon);

        final Label label = new Label(ID_LABEL, new LabelModel());
        label.add(visibleIfNotReference);
        add(label);

        final AjaxLinkPanel link = new AjaxLinkPanel(ID_LINK, new LabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!(getModelObject().getSourceValue() instanceof PrismReferenceValue)) {
                    return;
                }
                PrismReferenceValue refValue = (PrismReferenceValue) getModelObject().getSourceValue();
                if (refValue == null){
                    return;
                }
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setupReferenceValue(refValue);
                WebComponentUtil.dispatchToObjectDetailsPage(ort, getPageBase(), false);

            }
        };
        link.add(visibleIfReference);
        add(link);

        final Label additionalText = new Label(ID_ADDITIONAL_TEXT, new AdditionalLabelModel());
        add(additionalText);
    }

    private boolean hasValidReferenceValue(SceneItemValue object) {
        PrismReferenceValue target = null;
        if (object != null && object.getSourceValue() != null
                && object.getSourceValue() instanceof PrismReferenceValue
                && (object.getSourceValue() != null)) {
            target = (PrismReferenceValue) object.getSourceValue();
        }
        if (target == null) {
            return false;
        }

        QName targetType = target.getTargetType();
        if (target == null) {
            return false;
        }

        Class<? extends ObjectType> targetClass = getPrismContext().getSchemaRegistry().getCompileTimeClass(targetType);

        return WebComponentUtil.isAuthorized(targetClass);
    }

    private ObjectTypeGuiDescriptor getObjectTypeDescriptor() {
        SceneItemValue value = getModelObject();
        if (value != null && value.getSourceValue() != null && value.getSourceValue() instanceof PrismReferenceValue) {
            QName targetType = ((PrismReferenceValue) value.getSourceValue()).getTargetType();
            return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(targetType));
        } else {
            return null;
        }
    }

    private class LabelModel implements IModel<String> {
        @Override
        public String getObject() {
            SceneItemValue val = getModelObject();
            if (val == null) {
                return null;
            }
            if (val.getSourceValue() != null) {
                if (val.getSourceValue() instanceof PrismReferenceValue) {
                    return WebComponentUtil.getReferencedObjectDisplayNameAndName(((PrismReferenceValue) val.getSourceValue()).asReferencable(), true, getPageBase());
                } else if (val.getSourceValue() instanceof Objectable) {
                    WebComponentUtil.getDisplayNameOrName(((Objectable)val.getSourceValue()).asPrismObject());
                }
            }
            String textValue = getModelObject() != null ? getModelObject().getText() : null;
            if (textValue != null && textValue.isEmpty()) {
                textValue = createStringResource("SceneItemLinePanel.emptyLabel").getString();
            }
            return textValue;
        }
    }

    private class AdditionalLabelModel implements IModel<String> {
        @Override
        public String getObject() {
            SceneItemValue val = getModelObject();
            if (val == null) {
                return null;
            }
            if (val.getSourceValue() != null && val.getSourceValue() instanceof PrismReferenceValue) {
                return "[" + WebComponentUtil.getRelationLabelValue((PrismReferenceValue) val.getSourceValue(), getPageBase()) + "]";
            }

            LocalizationService service = MidPointApplication.get().getLocalizationService();
            String additionalText = getModelObject().getAdditionalText();
            return service.translate(additionalText, new Object[0], getPage().getLocale());
        }
    }
}
