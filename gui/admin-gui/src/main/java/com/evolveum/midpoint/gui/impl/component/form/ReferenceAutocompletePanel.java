/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.form;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteReferenceRenderer;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.ReferenceConverter;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;

import com.evolveum.midpoint.web.component.input.validator.ReferenceAutocompleteValidator;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Referencable;

import org.apache.wicket.util.convert.IConverter;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel for selecting object by autocomplete field for name of object and button for selecting of reference.
 */
public class ReferenceAutocompletePanel<R extends Referencable> extends ValueChoosePanel<R> {

    private static final String ID_EDIT_BUTTON_LABEL = "buttonLabel";

    ReferenceConverter<R> referenceConverter;

    public ReferenceAutocompletePanel(String id, IModel<R> value) {
        super(id, value);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initReferenceConverter();

        Label label = new Label(
                ID_EDIT_BUTTON_LABEL,
                getPageBase().createStringResource("ReferenceAutocompletePanel.button.select", getTypeTranslation()));
        label.add(new VisibleBehaviour(this::isButtonLabelVisible));
        getEditButton().add(AttributeAppender.append("title", () -> {
            if (!isButtonLabelVisible()) {
                return LocalizationUtil.translate("ReferenceAutocompletePanel.button.select", getTypeTranslation());
            }
            return null;
        }));
        getEditButton().add(label);
    }

    private void initReferenceConverter() {
        referenceConverter = new ReferenceConverter<>(getBaseFormComponent(), getPageBase()) {
            @Override
            protected List<Class<ObjectType>> getSupportedObjectTypes() {
                return ReferenceAutocompletePanel.this.getSupportedObjectTypes();
            }

            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return ReferenceAutocompletePanel.this.isAllowedNotFoundObjectRef();
            }

            @Override
            protected ObjectQuery createChooseQuery() {
                return ReferenceAutocompletePanel.this.createChooseQuery();
            }
        };
    }

    protected boolean isButtonLabelVisible() {
        return false;
    }

    private String getTypeTranslation() {
        List<QName> types = getSupportedTypes();
        ObjectTypes type = ObjectTypes.OBJECT;
        if (types.size() == 1
                && !QNameUtil.match(types.get(0), ObjectType.COMPLEX_TYPE)
                && ObjectTypes.getObjectTypeClassIfKnown(types.get(0)) != null) {
            type = ObjectTypes.getObjectTypeFromTypeQName(types.get(0));
        }
        return getPageBase().createStringResource(type).getString().toLowerCase();
    }

    @Override
    protected Component createTextPanel(String id) {
        AutoCompleteTextPanel<R> autoComplete = new AutoCompleteTextPanel<>
                (id, getModel(), (Class<R>) Referencable.class, (IAutoCompleteRenderer<R>) new AutoCompleteReferenceRenderer()) {

            @Override
            public Iterator<R> getIterator(String input) {
                FormComponent<R> inputField = getBaseFormComponent();
                String realInput = StringUtils.isEmpty(input) ?
                        (!inputField.hasRawInput() ? inputField.getValue() : inputField.getRawInput())
                        : input;
                if (StringUtils.isEmpty(realInput)) {
                    return Collections.emptyIterator();
                }

                Class<ObjectType> type = ObjectType.class;
                List<Class<ObjectType>> supportedTypes = getSupportedObjectTypes();
                if (supportedTypes.size() == 1) {
                    type = supportedTypes.iterator().next();
                }

                S_FilterExit filter = PrismContext.get().queryFor(type)
                        .item(ObjectType.F_NAME)
                        .containsPoly(realInput)
                        .matchingNorm();

                if (supportedTypes.size() > 1) {
                    for (Class<ObjectType> supportedType : supportedTypes) {
                        filter.and().type(supportedType);
                    }
                }

                ObjectQuery condition = createChooseQuery();
                if (condition != null) {
                    filter = filter
                            .and()
                            .filter(condition.getFilter());
                }
                ObjectQuery query = filter.build();
                query.setPaging(PrismContext.get().queryFactory().createPaging(0, getMaxRowsCount()));

                List<PrismObject<ObjectType>> objectsList = WebModelServiceUtils.searchObjects(type, query,
                        new OperationResult("searchObjects"), getPageBase());
                return (Iterator<R>) ObjectTypeUtil.objectListToReferences(objectsList).iterator();
            }

            @Override
            protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
                return (IConverter<C>) referenceConverter;
            }

            @Override
            protected boolean isShowChoicesVisible() {
                return false;
            }
        };

        autoComplete.getBaseFormComponent().add(new ReferenceAutocompleteValidator(autoComplete));

        autoComplete.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

        autoComplete.getBaseFormComponent().add(
                AttributeAppender.append("class", "border-top-right-radius:0;"));
        autoComplete.getBaseFormComponent().add(
                AttributeAppender.append("style", "border-top-right-radius:0; border-bottom-right-radius:0;"));
        autoComplete.getBaseFormComponent().add(AttributeAppender.append(
                "readonly", () -> isEditButtonEnabled() ? null : "readonly"));
        return autoComplete;
    }

    private List<Class<ObjectType>> getSupportedObjectTypes() {
        return ReferenceAutocompletePanel.this.getSupportedTypes().stream()
                .map(type -> WebComponentUtil.qnameToClass(type, ObjectType.class))
                .collect(Collectors.toList());
    }

    protected int getMaxRowsCount() {
        return 20;
    }

    @Override
    public FormComponent<String> getBaseFormComponent() {
        AutoCompleteTextPanel panel = (AutoCompleteTextPanel) getBaseComponent();
        return panel.getBaseFormComponent();
    }

    public AutoCompleteTextPanel getAutoCompleteField() {
        return (AutoCompleteTextPanel) getBaseComponent();
    }

    public ReferenceConverter<R> getConverter() {
        return referenceConverter;
    }

    protected boolean isAllowedNotFoundObjectRef(){
        return false;
    }
}
