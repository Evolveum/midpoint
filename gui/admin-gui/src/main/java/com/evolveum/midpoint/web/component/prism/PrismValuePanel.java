/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.input.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.RoleApprovalFormType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.PackageResourceReference;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismValuePanel extends Panel {

    private static final String ID_FEEDBACK = "feedback";

    private IModel<ValueWrapper> model;

    public PrismValuePanel(String id, IModel<ValueWrapper> model, IModel<String> label, Form form) {
        super(id);
        Validate.notNull(model, "Property value model must not be null.");
        this.model = model;

        initLayout(label, form);
    }

    private void initLayout(IModel<String> label, Form form) {
        //feedback
        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);

        //input
        InputPanel input = createInputComponent("input", label, form);
        initAccessBehaviour(input);
        add(input);

        feedback.setFilter(new ComponentFeedbackMessageFilter(input.getBaseFormComponent()));

        //buttons
        AjaxLink addButton = new AjaxLink("addButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        };
        addButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible();
            }
        });
        add(addButton);

        AjaxLink removeButton = new AjaxLink("removeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValue(target);
            }
        };
        removeButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        add(removeButton);
    }

    private IModel<String> createHelpModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyWrapper wrapper = model.getObject().getProperty();
                return wrapper.getItem().getHelp();
            }
        };
    }

    private boolean isAccessible(PrismPropertyDefinition def, ContainerStatus status) {
        switch (status) {
            case ADDING:
                if (!def.canAdd()) {
                    return false;
                }
                break;
            case MODIFYING:
                if (!def.canModify()) {
                    return false;
                }
                break;
        }

        return true;
    }

    private void initAccessBehaviour(InputPanel panel) {
        List<FormComponent> components = panel.getFormComponents();
        for (FormComponent component : components) {
            component.add(new VisibleEnableBehaviour() {

                @Override
                public boolean isEnabled() {
                    ValueWrapper wrapper = model.getObject();
                    PropertyWrapper propertyWrapper = wrapper.getProperty();
                    ObjectWrapper object = propertyWrapper.getContainer().getObject();
                    PrismPropertyDefinition def = propertyWrapper.getItem().getDefinition();

                    return !model.getObject().isReadonly() && isAccessible(def, object.getStatus());
                }
            });
        }
    }

    private int countUsableValues(PropertyWrapper property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            value.normalize();

            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }

            if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
                continue;
            }

            count++;
        }
        return count;
    }

    private List<ValueWrapper> getUsableValues(PropertyWrapper property) {
        List<ValueWrapper> values = new ArrayList<ValueWrapper>();
        for (ValueWrapper value : property.getValues()) {
            value.normalize();
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            values.add(value);
        }

        return values;
    }

    private int countNonDeletedValues(PropertyWrapper property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            value.normalize();
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean hasEmptyPlaceholder(PropertyWrapper property) {
        for (ValueWrapper value : property.getValues()) {
            value.normalize();
            if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
                return true;
            }
        }

        return false;
    }

    private boolean isRemoveButtonVisible() {
        ValueWrapper valueWrapper = model.getObject();
        PropertyWrapper propertyWrapper = valueWrapper.getProperty();
        PrismPropertyDefinition definition = propertyWrapper.getItem().getDefinition();
        int min = definition.getMinOccurs();

        int count = countNonDeletedValues(propertyWrapper);
        if (count <= 1 || count <= min) {
            return false;
        }

        return isAccessible(definition, propertyWrapper.getContainer().getObject().getStatus());
    }

    private boolean isAddButtonVisible() {
        ValueWrapper valueWrapper = model.getObject();
        PropertyWrapper propertyWrapper = valueWrapper.getProperty();
        PrismProperty property = propertyWrapper.getItem();

        PrismPropertyDefinition definition = property.getDefinition();
        int max = definition.getMaxOccurs();
        List<ValueWrapper> usableValues = getUsableValues(propertyWrapper);
        if (usableValues.indexOf(valueWrapper) != usableValues.size() - 1) {
            return false;
        }

        if (max == -1) {
            return true;
        }

        if (countNonDeletedValues(propertyWrapper) >= max) {
            return false;
        }

        return isAccessible(definition, propertyWrapper.getContainer().getObject().getStatus());
    }

    private InputPanel createInputComponent(String id, IModel<String> label, Form form) {
        ValueWrapper valueWrapper = model.getObject();
        ObjectWrapper objectWrapper = valueWrapper.getProperty().getContainer().getObject();
        PrismProperty property = valueWrapper.getProperty().getItem();
        boolean required = property.getDefinition().getMinOccurs() > 0;
        //enforcing required for user will be enabled later (MID-1048)
        if (UserType.class.isAssignableFrom(objectWrapper.getObject().getCompileTimeClass())) {
            required = false;
        }

        InputPanel component = createTypedInputComponent(id);

        //adding valid from/to date range validator, if necessary
        ItemPath activation = new ItemPath(UserType.F_ACTIVATION);
        if (ActivationType.F_VALID_FROM.equals(property.getElementName())) {
            DateValidator validator = getActivationRangeValidator(form, activation);
            validator.setDateFrom((DateTimeField) component.getBaseFormComponent());
        } else if (ActivationType.F_VALID_TO.equals(property.getElementName())) {
            DateValidator validator = getActivationRangeValidator(form, activation);
            validator.setDateTo((DateTimeField) component.getBaseFormComponent());
        }

        final List<FormComponent> formComponents = component.getFormComponents();
        for (FormComponent formComponent : formComponents) {
            formComponent.setLabel(label);
            formComponent.setRequired(required);

            if (formComponent instanceof TextField) {
                formComponent.add(new AttributeModifier("size", "42"));
            }
            formComponent.add(new AjaxFormComponentUpdatingBehavior("onBlur") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                }
            });

            // Validation occurs when submitting the form
//            if (form != null) {
//                AjaxFormValidatingBehavior validator = new AjaxFormValidatingBehavior(form, "onBlur"); 
//                 
//                formComponent.add(validator);
//            }
        }
        return component;
    }

    private DateValidator getActivationRangeValidator(Form form, ItemPath path) {
        DateValidator validator = null;
        List<DateValidator> validators = form.getBehaviors(DateValidator.class);
        if (validators != null) {
            for (DateValidator val : validators) {
                if (path.equivalent(val.getIdentifier())) {
                    validator = val;
                    break;
                }
            }
        }

        if (validator == null) {
            validator = new DateValidator();
            validator.setIdentifier(path);
            form.add(validator);
        }

        return validator;
    }

    private InputPanel createTypedInputComponent(String id) {
        PrismProperty property = model.getObject().getProperty().getItem();
        PrismPropertyDefinition definition = property.getDefinition();
        QName valueType = definition.getTypeName();

        final String baseExpression = "value.value"; //pointing to prism property real value

        //fixing MID-1230, will be improved with some kind of annotation or something like that
        //now it works only in description
        if (ObjectType.F_DESCRIPTION.equals(definition.getName())) {
            return new TextAreaPanel(id, new PropertyModel(model, baseExpression));
        }

        // the same for requester and approver comments in workflows [mederly] - this is really ugly, as it is specific to each approval form
        if (RoleApprovalFormType.F_REQUESTER_COMMENT.equals(definition.getName()) || RoleApprovalFormType.F_COMMENT.equals(definition.getName())) {
            return new TextAreaPanel(id, new PropertyModel(model, baseExpression));
        }

        InputPanel panel;
        if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(definition.getName())) {
            return WebMiscUtil.createActivationStatusPanel(id, new PropertyModel<ActivationStatusType>(model, baseExpression), this);
        } else if(ActivationType.F_LOCKOUT_STATUS.equals(definition.getName())){
            return WebMiscUtil.createLockoutStatsPanel(id, new PropertyModel<LockoutStatusType>(model, baseExpression), this);
        }

        if (DOMUtil.XSD_DATETIME.equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(model, baseExpression));
        } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
            panel = new PasswordPanel(id, new PropertyModel<String>(model, baseExpression + ".clearValue"));
        } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
            panel = new TriStateComboPanel(id, new PropertyModel<Boolean>(model, baseExpression));
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
            panel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression + ".orig"), String.class);

            PrismPropertyDefinition def = property.getDefinition();
            if (ObjectType.F_NAME.equals(def.getName()) || UserType.F_FULL_NAME.equals(def.getName())) {
                panel.getBaseFormComponent().setRequired(true);
            }
        } else if(DOMUtil.XSD_BASE64BINARY.equals(valueType)){
            panel = new UploadPanel(id){

                @Override
                public void updateValue(byte[] file) {
                    model.getObject().getValue().setValue(file);
                }

                @Override
                public void uploadFilePerformed(AjaxRequestTarget target) {
                    super.uploadFilePerformed(target);
                    target.add(PrismValuePanel.this.get(ID_FEEDBACK));
                }

                @Override
                public void removeFilePerformed(AjaxRequestTarget target) {
                    super.removeFilePerformed(target);
                    target.add(PrismValuePanel.this.get(ID_FEEDBACK));
                }

                @Override
                public void uploadFileFailed(AjaxRequestTarget target) {
                    super.uploadFileFailed(target);
                    target.add(PrismValuePanel.this.get(ID_FEEDBACK));
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                }
            };

        } else {
            Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
            if (type != null && type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            panel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression), type);
        }

        return panel;
    }

    private void addValue(AjaxRequestTarget target) {
        ValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();
        propertyWrapper.addValue();

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

    private void removeValue(AjaxRequestTarget target) {
        ValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();

        List<ValueWrapper> values = propertyWrapper.getValues();

        switch (wrapper.getStatus()) {
            case ADDED:
                values.remove(wrapper);
                break;
            case DELETED:
                error("Couldn't delete already deleted item: " + wrapper.toString());
                target.add(((PageBase) getPage()).getFeedbackPanel());
            case NOT_CHANGED:
                wrapper.setStatus(ValueStatus.DELETED);
                break;
        }

        int count = countUsableValues(propertyWrapper);
        if (count == 0 && !hasEmptyPlaceholder(propertyWrapper)) {
            values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));
        }

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }
}
