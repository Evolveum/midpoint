/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.PrismValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO make this parametric (along with VisualizationItemValue)
 */
public class VisualizationItemValuePanel extends BasePanel<VisualizationItemValue> {

    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_LINK = "link";
    private static final String ID_ADDITIONAL_TEXT = "additionalText";

    public VisualizationItemValuePanel(String id, IModel<VisualizationItemValue> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final VisibleBehaviour visibleIfReference = new VisibleBehaviour(() -> {
            VisualizationItemValue object = getModelObject();
            return hasValidReferenceValue(object);
        });
        final VisibleBehaviour visibleIfNotReference = new VisibleBehaviour(() -> {
            VisualizationItemValue object = getModelObject();
            return !hasValidReferenceValue(object);
        });

        final IconComponent icon = new IconComponent(ID_ICON,
                () -> {
                    ObjectTypeGuiDescriptor descriptor = getObjectTypeDescriptor();
                    return descriptor != null ? descriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                },
                () -> {
                    ObjectTypeGuiDescriptor descriptor = getObjectTypeDescriptor();
                    return descriptor != null ? LocalizationUtil.translate(descriptor.getLocalizationKey()) : null;
                });
        icon.add(visibleIfReference);
        add(icon);

        final Label label = new Label(ID_LABEL, new LabelModel());
        label.add(visibleIfNotReference);
        add(label);

        final AjaxButton link = new AjaxButton(ID_LINK, new LabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!(VisualizationItemValuePanel.this.getModelObject().getSourceValue() instanceof PrismReferenceValue)) {
                    return;
                }
                PrismReferenceValue refValue = (PrismReferenceValue) VisualizationItemValuePanel.this.getModelObject().getSourceValue();
                if (refValue == null || refValue.getOid() == null) {
                    return;
                }
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setupReferenceValue(refValue);
                DetailsPageUtil.dispatchToObjectDetailsPage(ort, getPageBase(), false);
            }
        };
        link.add(visibleIfReference);
        add(link);

        final Label additionalText = new Label(ID_ADDITIONAL_TEXT, new AdditionalLabelModel());
        add(additionalText);
    }

    private boolean hasValidReferenceValue(VisualizationItemValue object) {
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
        VisualizationItemValue value = getModelObject();
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
            VisualizationItemValue val = getModelObject();
            if (val == null) {
                return null;
            }
            PrismValue prismvalue = val.getSourceValue();
            if (prismvalue != null) {
                if (prismvalue instanceof PrismReferenceValue) {
                    PrismReferenceValue ref = (PrismReferenceValue) prismvalue;
                    if (ref.getOid() == null) {
                        return LocalizationUtil.translate("VisualizationItemValue.undefinedOid");
                    }

                    return WebComponentUtil.getReferencedObjectDisplayNameAndName(ref.asReferencable(), true, getPageBase());
                } else if (prismvalue instanceof Objectable) {
                    return WebComponentUtil.getDisplayNameOrName(((Objectable) prismvalue).asPrismObject());
                }
            }
            LocalizableMessage textValue = getModelObject().getText();
            if (textValue == null) {
                return null;
            }

            String value = WebComponentUtil.translateMessage(textValue);
            if (StringUtils.isEmpty(value)) {
                return getString("SceneItemLinePanel.emptyLabel");
            }

            return value;
        }
    }

    private class AdditionalLabelModel implements IModel<String> {
        @Override
        public String getObject() {
            VisualizationItemValue val = getModelObject();
            if (val == null) {
                return null;
            }
            if (val.getSourceValue() != null && val.getSourceValue() instanceof PrismReferenceValue) {
                return "[" + RelationUtil.getRelationLabelValue((PrismReferenceValue) val.getSourceValue(), getPageBase()) + "]";
            }
            return WebComponentUtil.translateMessage(getModelObject().getAdditionalText());
        }
    }
}
