/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.impl.converter.QNameConverter;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.convert.IConverter;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author katkav
 */
public abstract class AbstractQNameWithChoicesPanelFactory extends QNameTextPanelFactory implements Serializable{

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        if (applyAutocomplete(panelCtx)) {

            IModel<List<DisplayableValue<QName>>> values = new LoadableDetachableModel<>() {
                @Override
                protected List<DisplayableValue<QName>> load() {
                    return createValues(panelCtx);
                }
            };


                IAutoCompleteRenderer<QName> renderer = new AbstractAutoCompleteRenderer<>() {
                    @Override
                    protected void renderChoice(QName itemName, Response response, String s) {
                        response.write(getTextValue(itemName));
                    }

                    @Override
                    protected String getTextValue(QName itemName) {
                        return values.getObject().stream()
                                .filter(attr -> QNameUtil.match(attr.getValue(), itemName))
                                .findFirst()
                                .get().getLabel();
                    }
                };

                AutoCompleteTextPanel panel = new AutoCompleteTextPanel<>(
                        panelCtx.getComponentId(), panelCtx.getRealValueModel(), panelCtx.getTypeClass(), renderer) {
                    @Override
                    public Iterator<QName> getIterator(String input) {
                        List<DisplayableValue<QName>> choices = new ArrayList<>(values.getObject());
                        if (StringUtils.isNotEmpty(input)) {
                            String partOfInput;
                            if (input.contains(":")) {
                                partOfInput = input.substring(input.indexOf(":") + 1);
                            } else {
                                partOfInput = input;
                            }
                            choices = choices.stream()
                                    .filter(v -> StringUtils.containsIgnoreCase(v.getLabel(), partOfInput))
                                    .collect(Collectors.toList());
                        }
                        return choices.stream()
                                .map(v -> v.getValue())
                                .collect(Collectors.toList())
                                .iterator();
                    }

                    @Override
                    protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
                        return (IConverter<C>) new QNameConverter(values, isStrictForPossibleValues());
                    }
                };
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                return panel;
            }
        return super.getPanel(panelCtx);
    }

    protected boolean isStrictForPossibleValues() {
        return false;
    }

    protected abstract List<DisplayableValue<QName>> createValues(PrismPropertyPanelContext<QName> panelCtx);

    protected boolean applyAutocomplete(PrismPropertyPanelContext<QName> panelCtx) {
        return true;
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    private class AssociationDisplayableValue implements DisplayableValue<QName>, Serializable{

        private final String displayName;
        private final String help;
        private final QName value;

        private AssociationDisplayableValue(ShadowAttributeDefinition attributeDefinition) {
            this.displayName = attributeDefinition.getDisplayName() == null ?
                    attributeDefinition.getItemName().getLocalPart() : attributeDefinition.getDisplayName();
            this.help = attributeDefinition.getHelp();
            this.value = attributeDefinition.getItemName();
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
            return help;
        }
    }

}
