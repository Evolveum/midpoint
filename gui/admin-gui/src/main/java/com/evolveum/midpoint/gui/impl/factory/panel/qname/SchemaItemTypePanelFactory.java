/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SchemaItemTypePanelFactory extends AbstractQNameWithChoicesPanelFactory implements Serializable {

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<QName> panelCtx) {
        if (panelCtx.unwrapWrapperModel() != null && (panelCtx.unwrapWrapperModel().isReadOnly() || !panelCtx.isEditable())) {

            LoadableDetachableModel<String> model = new LoadableDetachableModel<>() {
                @Override
                protected String load() {
                    List<DisplayableValue<QName>> values = createValues(panelCtx);
                    Optional<DisplayableValue<QName>> qNameValue = values.stream()
                            .filter(value -> QNameUtil.match(value.getValue(), panelCtx.getRealValueModel().getObject()))
                            .findFirst();
                    if (qNameValue.isPresent()) {
                        return qNameValue.get().getLabel();
                    }
                    return panelCtx.getRealValueStringModel().getObject();
                }
            };
            return new Label(panelCtx.getComponentId(), model);
        }
        return super.createPanel(panelCtx);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(PrismItemDefinitionType.class) != null
                && PrismItemDefinitionType.F_TYPE.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    protected List<DisplayableValue<QName>> createValues(PrismPropertyPanelContext<QName> panelCtx) {
        return getAllTypes(panelCtx.getValueWrapperModel());
    }

    private List<DisplayableValue<QName>> getAllTypes(IModel<? extends PrismValueWrapper<QName>> propertyWrapperModel) {

        List<DisplayableValue<QName>> alltypes = new ArrayList<>();

        XsdTypeMapper.getAllTypes().forEach(type -> alltypes.add(createDisplayValue(createLabelForType(null, type), type)));

        alltypes.add(new TypeDisplayableValue("ref.details", ObjectReferenceType.COMPLEX_TYPE));

        PrismContext.get().getSchemaRegistry().getPrismSchema(SchemaConstants.NS_C).getDefinitions().forEach(definition -> {
            if (definition instanceof EnumerationTypeDefinition) {
                alltypes.add(new TypeDisplayableValue(
                        createLabelForEnum(definition.getDisplayName(), definition.getTypeName()),
                        definition.getTypeName()));
                return;
            }

            if (definition instanceof ComplexTypeDefinition complexTypeDef
                    && !complexTypeDef.isObjectMarker()
                    && !complexTypeDef.isReferenceMarker()) {
                alltypes.add(new TypeDisplayableValue(
                        createLabelForType(definition.getDisplayName(), definition.getTypeName()),
                        definition.getTypeName()));
            }
        });

        PrismValueWrapper<QName> propertyWrapper = propertyWrapperModel.getObject();
        if (propertyWrapper == null) {
            return alltypes;
        }

        PrismContainerValueWrapper<PrismSchemaType> schema = propertyWrapper.getParentContainerValue(PrismSchemaType.class);
        if (schema == null || schema.getRealValue() == null) {
            return alltypes;
        }

        schema.getRealValue().getComplexType().stream()
                .filter(complexType -> complexType.getExtension() == null)
                .forEach(complexType -> alltypes.add(new TypeDisplayableValue(
                        createLabelForType(complexType.getDisplayName(), complexType.getName()),
                        complexType.getName())));

        schema.getRealValue().getEnumerationType().stream()
                .forEach(enumType -> alltypes.add(new TypeDisplayableValue(
                        createLabelForEnum(enumType.getDisplayName(), enumType.getName()),
                        enumType.getName())));

        return alltypes;
    }

    protected DisplayableValue<QName> createDisplayValue(String labelForType, QName type) {
        return new TypeDisplayableValue(labelForType, type);
    }

    private String createLabelForEnum(String displayName, QName typeName) {
        String label = createLabelForType(displayName, typeName);
        label = StringUtils.removeEndIgnoreCase(label, "type");
        label = label.trim();
        return LocalizationUtil.translate("SchemaItemTypePanelFactory.enumeration", new Object[] { label });
    }

    String createLabelForType(String displayName, QName typeName) {
        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }

        String label = typeName.getLocalPart();
        label = StringUtils.removeEndIgnoreCase(label, "type");
        String[] partsOfLabel = StringUtils.splitByCharacterTypeCamelCase(label);
        label = StringUtils.joinWith(" ", partsOfLabel);
        label = label.toLowerCase();
        return StringUtils.capitalize(label);
    }

    @Override
    public Integer getOrder() {
        return 99;
    }

    @Override
    protected boolean isStrictForPossibleValues() {
        return true;
    }

    @Override
    public void configure(PrismPropertyPanelContext<QName> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);
        if (component instanceof Label) {
            panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(component));
        }
    }

    private class TypeDisplayableValue implements DisplayableValue<QName>, Serializable {

        private final String displayName;
        private final String namespace;
        private final QName value;

        private TypeDisplayableValue(@NotNull String label, @NotNull QName type) {
            this.displayName = LocalizationUtil.translate(label, new Object[] {}, StringUtils.capitalize(label));
            this.value = type;
            this.namespace = type.getNamespaceURI();
        }

        @Override
        public QName getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return namespace;
        }
    }

}
