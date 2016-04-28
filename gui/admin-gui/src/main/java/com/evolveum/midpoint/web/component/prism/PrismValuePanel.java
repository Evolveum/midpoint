/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.*;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.ModificationsPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.component.AssociationValueChoicePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismValuePanel extends Panel {

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_INPUT = "input";
    private static final String ID_VALUE_CONTAINER = "valueContainer";
    private static final String DOT_CLASS = PrismValuePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSOC_SHADOWS =  DOT_CLASS + "loadAssociationShadows";
    
    private static final Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

    private IModel<ValueWrapper> model;
    private PageBase pageBase;

    public PrismValuePanel(String id, IModel<ValueWrapper> model, IModel<String> label, Form form,
                           String valueCssClass, String inputCssClass, PageBase pageBase){
        super(id);
        Validate.notNull(model, "Property value model must not be null.");
        Validate.notNull(pageBase, "The reference to page base must not be null.");
        this.pageBase = pageBase;
        this.model = model;

        initLayout(label, form, valueCssClass, inputCssClass);
    }

    private void initLayout(IModel<String> label, Form form, String valueCssClass, String inputCssClass) {
        //container
        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        valueContainer.add(new AttributeModifier("class", valueCssClass));
        add(valueContainer);

        //feedback
        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);

        //input
        Panel input = createInputComponent(ID_INPUT, label, form);
        input.add(new AttributeModifier("class", inputCssClass));
        if (input instanceof InputPanel) {
            initAccessBehaviour((InputPanel) input);
            feedback.setFilter(new ComponentFeedbackMessageFilter(((InputPanel) input).getBaseFormComponent()));
        }
        valueContainer.add(input);

        //buttons
        AjaxLink addButton = new AjaxLink("addButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                Component inputPanel = this.getParent().get(ID_INPUT);
                addValue(target);
            }
        };
        addButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible();
            }
        });
        valueContainer.add(addButton);

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
        valueContainer.add(removeButton);
    }

    private IModel<String> createHelpModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ItemWrapper wrapper = model.getObject().getItem();
                return wrapper.getItem().getHelp();
            }
        };
    }

    private boolean isAccessible(ItemDefinition def, ContainerStatus status) {
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
                    ItemWrapper itemWrapper = wrapper.getItem();
                    if (itemWrapper.getContainer() == null) {
                        return true;        // TODO
                    }
                    ObjectWrapper object = itemWrapper.getContainer().getObject();
                    ItemDefinition def = itemWrapper.getItem().getDefinition();

                    return !model.getObject().isReadonly() && (object == null || isAccessible(def, object.getStatus()));
                }
            });
        }
    }

    private int countUsableValues(ItemWrapper<? extends Item, ? extends ItemDefinition> property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            value.normalize(property.getItemDefinition().getPrismContext());

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

    private List<ValueWrapper> getUsableValues(ItemWrapper<? extends Item, ? extends ItemDefinition> property) {
        List<ValueWrapper> values = new ArrayList<>();
        for (ValueWrapper value : property.getValues()) {
            value.normalize(property.getItemDefinition().getPrismContext());
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            values.add(value);
        }

        return values;
    }

    private int countNonDeletedValues(ItemWrapper<? extends Item, ? extends ItemDefinition> property) {
        int count = 0;
        for (ValueWrapper value : property.getValues()) {
            value.normalize(property.getItemDefinition().getPrismContext());
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean hasEmptyPlaceholder(ItemWrapper<? extends Item, ? extends ItemDefinition> property) {
        for (ValueWrapper value : property.getValues()) {
            value.normalize(property.getItemDefinition().getPrismContext());
            if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
                return true;
            }
        }

        return false;
    }

    private boolean isRemoveButtonVisible() {
        ValueWrapper valueWrapper = model.getObject();

        if (valueWrapper.isReadonly()){
            return false;
        }
        Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
        if (inputPanel instanceof  ValueChoosePanel || inputPanel instanceof AssociationValueChoicePanel){
            return true;
        }

        ItemWrapper propertyWrapper = valueWrapper.getItem();
        ItemDefinition definition = propertyWrapper.getItem().getDefinition();
        int min = definition.getMinOccurs();

        int count = countNonDeletedValues(propertyWrapper);
        if (count <= 1 || count <= min) {
            return false;
        }

        if (propertyWrapper.getContainer() == null) {
            return true;        // TODO
        }

		return isAccessible(definition, getContainerStatus(propertyWrapper));
    }

	private ContainerStatus getContainerStatus(ItemWrapper propertyWrapper) {
		final ObjectWrapper objectWrapper = propertyWrapper.getContainer().getObject();
		return objectWrapper != null ? objectWrapper.getStatus() : ContainerStatus.MODIFYING;
	}

	private boolean isAddButtonVisible() {
        Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
        ValueWrapper valueWrapper = model.getObject();

        if (valueWrapper.isReadonly()){
            return false;
        }

        ItemWrapper propertyWrapper = valueWrapper.getItem();
        Item property = propertyWrapper.getItem();

        ItemDefinition definition = property.getDefinition();
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

        if (propertyWrapper.getContainer() == null) {
            return true;            // TODO
        }
        return isAccessible(definition, getContainerStatus(propertyWrapper));
    }

    private Panel createInputComponent(String id, IModel<String> label, Form form) {
        ValueWrapper valueWrapper = model.getObject();
        ObjectWrapper objectWrapper = null;
        if (valueWrapper.getItem().getContainer() != null) {
            objectWrapper = valueWrapper.getItem().getContainer().getObject();
        }
        Item property = valueWrapper.getItem().getItem();
        boolean required = property.getDefinition().getMinOccurs() > 0;

        Panel component = createTypedInputComponent(id);

        if (component instanceof InputPanel) {
            InputPanel inputPanel = (InputPanel) component;
            //adding valid from/to date range validator, if necessary
            ItemPath activation = new ItemPath(UserType.F_ACTIVATION);
            if (ActivationType.F_VALID_FROM.equals(property.getElementName())) {
                DateValidator validator = getActivationRangeValidator(form, activation);
                validator.setDateFrom((DateTimeField) inputPanel.getBaseFormComponent());
            } else if (ActivationType.F_VALID_TO.equals(property.getElementName())) {
                DateValidator validator = getActivationRangeValidator(form, activation);
                validator.setDateTo((DateTimeField) inputPanel.getBaseFormComponent());
            }

            final List<FormComponent> formComponents = inputPanel.getFormComponents();
            for (FormComponent formComponent : formComponents) {
                formComponent.setLabel(label);
                formComponent.setRequired(required);

                if (formComponent instanceof TextField) {
                    formComponent.add(new AttributeModifier("size", "42"));
                }
                formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }
                });

                // Validation occurs when submitting the form
//            if (form != null) {
//                AjaxFormValidatingBehavior validator = new AjaxFormValidatingBehavior(form, "Blur"); 
//                 
//                formComponent.add(validator);
//            }
            }
        }
        if (component == null) {
        	throw new RuntimeException("Cannot create input component for item "+property+" ("+valueWrapper+") in "+objectWrapper);
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

    // normally this method returns an InputPanel;
    // however, for some special readonly types (like ObjectDeltaType) it will return a Panel
    private Panel createTypedInputComponent(String id) {
//        ValueWrapper valueWrapper = model.getObject();
//        ItemWrapper itemWrapper =
        final Item item = model.getObject().getItem().getItem();
        
        Panel panel = null;
        if (item instanceof PrismProperty) {
        	  final PrismProperty property = (PrismProperty) item;
        	  PrismPropertyDefinition definition = property.getDefinition();
              final QName valueType = definition.getTypeName();

              final String baseExpression = "value.value"; //pointing to prism property real value

              //fixing MID-1230, will be improved with some kind of annotation or something like that
              //now it works only in description
              if (ObjectType.F_DESCRIPTION.equals(definition.getName())) {
                  return new TextAreaPanel(id, new PropertyModel(model, baseExpression), null);
              }

              if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(definition.getName())) {
                  return WebComponentUtil.createEnumPanel(ActivationStatusType.class, id, new PropertyModel<ActivationStatusType>(model, baseExpression), this);
              } else if(ActivationType.F_LOCKOUT_STATUS.equals(definition.getName())){
                  return new LockoutStatusPanel(id, new PropertyModel<LockoutStatusType>(model, baseExpression));
              } else {
              	// nothing to do
              }
              
              if (DOMUtil.XSD_DATETIME.equals(valueType)) {
                  panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(model, baseExpression));
                  
              } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
                  boolean showRemovePasswordButton = true;
				  if (pageBase instanceof PageUser &&
						  ((PageUser) pageBase).getObjectWrapper().getObject() != null &&
						  ((PageUser) pageBase).getObjectWrapper().getObject().getOid() != null &&
						  ((PageUser) pageBase).getObjectWrapper().getObject().getOid().equals(SecurityUtils.getPrincipalUser().getOid())) {
                      showRemovePasswordButton = false;
                  }
                  panel = new PasswordPanel(id, new PropertyModel<ProtectedStringType>(model, baseExpression),
                          model.getObject().isReadonly(), showRemovePasswordButton);
              } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
                  panel = new TriStateComboPanel(id, new PropertyModel<Boolean>(model, baseExpression));
                  
              } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
                  InputPanel inputPanel;
                  PrismPropertyDefinition def = property.getDefinition();

                  if(def.getValueEnumerationRef() != null){
                      PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
                      String lookupTableUid = valueEnumerationRef.getOid();
                      Task task = pageBase.createSimpleTask("loadLookupTable");
                      OperationResult result = task.getResult();

                      Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                              GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
                      final PrismObject<LookupTableType> lookupTable = WebModelServiceUtils.loadObject(LookupTableType.class,
                              lookupTableUid, options, pageBase, task, result);

                      if (lookupTable != null) {
                    	  
	                      inputPanel = new AutoCompleteTextPanel<String>(id, new LookupPropertyModel<String>(model, baseExpression + ".orig",
	                              lookupTable.asObjectable()), String.class) {
	
	                          @Override
	                          public Iterator<String> getIterator(String input) {
	                              return prepareAutoCompleteList(input, lookupTable).iterator();
	                          }
	                      };
	                      
                      } else {
                    	  inputPanel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression + ".orig"), String.class);
                      }

                  } else {
                	  
                      inputPanel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression + ".orig"), String.class);
                  }

                  if (ObjectType.F_NAME.equals(def.getName()) || UserType.F_FULL_NAME.equals(def.getName())) {
                      inputPanel.getBaseFormComponent().setRequired(true);
                  }
                  panel = inputPanel;
                  
              } else if(DOMUtil.XSD_BASE64BINARY.equals(valueType)) {
                  panel = new UploadDownloadPanel(id, model.getObject().isReadonly()){

                	  
                	  @Override
                	public InputStream getStream() {
                          Object object  = ((PrismPropertyValue) model.getObject().getValue()).getValue();
                		return object != null ? new ByteArrayInputStream((byte[]) object) : new ByteArrayInputStream(new byte[0]);
//                		return super.getStream();
                	}
                	  
                      @Override
                      public void updateValue(byte[] file) {
                          ((PrismPropertyValue) model.getObject().getValue()).setValue(file);
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

              } else if (ObjectDeltaType.COMPLEX_TYPE.equals(valueType)) {
                  panel = new ModificationsPanel(id, new AbstractReadOnlyModel<DeltaDto>() {
                      @Override
                      public DeltaDto getObject() {
                          if (model.getObject() == null || model.getObject().getValue() == null || ((PrismPropertyValue) model.getObject().getValue()).getValue() == null) {
                              return null;
                          }
                          PrismContext prismContext = ((PageBase) getPage()).getPrismContext();
                          ObjectDeltaType objectDeltaType = (ObjectDeltaType) ((PrismPropertyValue) model.getObject().getValue()).getValue();
                          try {
                              ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
                              return new DeltaDto(delta);
                          } catch (SchemaException e) {
                              throw new IllegalStateException("Couldn't convert object delta: " + objectDeltaType);
                          }

                      }
                  });
              } else if (QueryType.COMPLEX_TYPE.equals(valueType) || CleanupPoliciesType.COMPLEX_TYPE.equals(valueType)) {
				  return new TextAreaPanel(id, new AbstractReadOnlyModel() {
					  @Override
					  public Object getObject() {
						  if (model.getObject() == null || model.getObject().getValue() == null) {
							  return null;
						  }
						  PrismPropertyValue ppv = (PrismPropertyValue) model.getObject().getValue();
						  if (ppv == null || ppv.getValue() == null) {
							  return null;
						  }
						  QName name = property.getElementName();
						  if (name == null && property.getDefinition() != null) {
							  name = property.getDefinition().getName();
						  }
						  if (name == null) {
							  name = SchemaConstants.C_VALUE;
						  }
						  PrismContext prismContext = ((PageBase) getPage()).getPrismContext();
						  try {
							  return prismContext.serializeAnyData(ppv.getValue(), name, PrismContext.LANG_XML);
						  } catch (SchemaException e) {
							  throw new SystemException("Couldn't serialize property value of type: " + valueType + ": " + e.getMessage(), e);
						  }
					  }
				  }, 10);
			  } else {
                  Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
                  if (type != null && type.isPrimitive()) {
                      type = ClassUtils.primitiveToWrapper(type);
                      
                  } 
                  
                  if (isEnum(property)) {
                      return WebComponentUtil.createEnumPanel(definition, id, new PropertyModel<>(model, baseExpression), this);
                  }
//                  // default QName validation is a bit weird, so let's treat QNames as strings [TODO finish this - at the parsing side]
//                  if (type == QName.class) {
//                      type = String.class;
//                  }

                  PrismPropertyDefinition def = property.getDefinition();

                  if(def.getValueEnumerationRef() != null){
                      PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
                      String lookupTableUid = valueEnumerationRef.getOid();
                      Task task = pageBase.createSimpleTask("loadLookupTable");
                      OperationResult result = task.getResult();

                      Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                              GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
                      final PrismObject<LookupTableType> lookupTable = WebModelServiceUtils.loadObject(LookupTableType.class,
                              lookupTableUid, options, pageBase, task, result);

                      if (lookupTable != null) {
                      
	                      panel = new AutoCompleteTextPanel<String>(id, new LookupPropertyModel<String>(model, baseExpression,
	                              lookupTable == null ? null : lookupTable.asObjectable()), type) {
	
	
	                          @Override
	                          public Iterator<String> getIterator(String input) {
	                              return prepareAutoCompleteList(input, lookupTable).iterator();
	                          }
	
	                          @Override
	                          public void checkInputValue(AutoCompleteTextField input, AjaxRequestTarget target, LookupPropertyModel model){
	                              Iterator<String> lookupTableValuesIterator = prepareAutoCompleteList("", lookupTable).iterator();
	
	                              String value = input.getInput();
	                              boolean isValueExist = false;
	                              if (value != null) {
	                                  if (value.trim().equals("")){
	                                      isValueExist = true;
	                                  } else {
	                                      while (lookupTableValuesIterator.hasNext()) {
	                                          String lookupTableValue = lookupTableValuesIterator.next();
	                                          if (value.trim().equals(lookupTableValue)) {
	                                              isValueExist = true;
	                                              break;
	                                          }
	                                      }
	                                  }
	                              }
	                              if (isValueExist){
	                                  input.setModelValue(new String[]{value});
	                                  target.add(PrismValuePanel.this.get(ID_FEEDBACK));
	                              } else {
	                                  input.error("Entered value doesn't match any of available values and will not be saved.");
	                                  target.add(PrismValuePanel.this.get(ID_FEEDBACK));
	                              }
	                          }
	                      };
	                      
                      } else {
                    	  
                    	  panel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression), type);
                    	  
                      }

                  } else {
                      panel = new TextPanel<>(id, new PropertyModel<String>(model, baseExpression), type);
                  }
              }
        } else if (item instanceof PrismReference) {
//        	((PrismReferenceDefinition) item.getDefinition()).
        	Class typeFromName = null;
        	PrismContext prismContext = item.getPrismContext();
            if (prismContext == null) {
                prismContext = pageBase.getPrismContext();
            }
            QName targetTypeName = ((PrismReferenceDefinition) item.getDefinition()).getTargetTypeName();
            if (targetTypeName != null && prismContext != null) {
                typeFromName = prismContext.getSchemaRegistry().determineCompileTimeClass(targetTypeName);
        	}
        	final Class typeClass = typeFromName != null ? typeFromName : (item.getDefinition().getTypeClassIfKnown() != null ? item.getDefinition().getTypeClassIfKnown() : FocusType.class);
        	panel = new ValueChoosePanel(id,
    				new PropertyModel<>(model, "value"), item.getValues(), false, typeClass);
        	
        } else if (item instanceof PrismContainer<?>) {
        	ItemWrapper itemWrapper = model.getObject().getItem();
        	final PrismContainer container = (PrismContainer) item;
        	PrismContainerDefinition definition = container.getDefinition();
            QName valueType = definition.getTypeName();
        	
            if (ShadowAssociationType.COMPLEX_TYPE.equals(valueType)) {
		
	            PrismContext prismContext = item.getPrismContext();
	            if (prismContext == null) {
	                prismContext = pageBase.getPrismContext();
	            }
	            
	            ShadowType shadowType = ((ShadowType)itemWrapper.getContainer().getObject().getObject().asObjectable());
	            PrismObject<ResourceType> resource = shadowType.getResource().asPrismObject();
	            // HACK. The revive should not be here. Revive is no good. The next use of the resource will
	            // cause parsing of resource schema. We need some centralized place to maintain live cached copies
	            // of resources.
	            try {
					resource.revive(prismContext);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}
	            RefinedResourceSchema refinedSchema;
	            CompositeRefinedObjectClassDefinition rOcDef;
	            try {
					refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
					rOcDef = refinedSchema.determineCompositeObjectClassDefinition(shadowType.asPrismObject());
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(),e);
				}
	            RefinedAssociationDefinition assocDef = rOcDef.findAssociation(itemWrapper.getName());
	            RefinedObjectClassDefinition assocTarget = assocDef.getAssociationTarget();
	            
	            ObjectQuery query = getAssociationsSearchQuery(prismContext, resource,
	            		assocTarget.getTypeName(), assocTarget.getKind(), assocTarget.getIntent());
	
	            List values = item.getValues();
	            return new AssociationValueChoicePanel(id, model, values, false, ShadowType.class, query);
            }
        }

        return panel;
    }

    private List<String> prepareAutoCompleteList(String input, PrismObject<LookupTableType> lookupTable){
        List<String> values = new ArrayList<>();

        if(lookupTable == null){
            return values;
        }

        List<LookupTableRowType> rows = lookupTable.asObjectable().getRow();

        if(input == null || input.isEmpty()){
            for(LookupTableRowType row: rows){
                values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));

                if(values.size() > 10){
                    return values;
                }
            }
        } else {
            for(LookupTableRowType row: rows){
                if(WebComponentUtil.getOrigStringFromPoly(row.getLabel()).startsWith(input)){
                    values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));
                }

                if(values.size() > 10){
                    return values;
                }
            }
        }

        return values;
    }

    private boolean isEnum(PrismProperty property){
    	PrismPropertyDefinition definition = property.getDefinition();
////    	Object realValue = property.getAnyRealValue();
    	if (definition == null){
    		return property.getValueClass().isEnum();
    	} 
//    	
//    	QName defName = definition.getName();
//    	Class clazz = definition.getPrismContext().getSchemaRegistry().determineCompileTimeClass(defName);
//    	
//    	return ((clazz != null && clazz.isEnum()) || ActivationType.F_ADMINISTRATIVE_STATUS.equals(definition.getName()) 
//    	 || ActivationType.F_LOCKOUT_STATUS.equals(definition.getName()) || );
    	return (definition.getAllowedValues() != null && definition.getAllowedValues().size() > 0);
    }
    //TODO - try to get rid of <br> attributes when creating new lines in association attributes pop-up
    private String createAssociationTooltipText(PrismProperty property){
        StringBuilder sb = new StringBuilder();
        sb.append(getString("prismValuePanel.message.association.attributes")).append("<br>");

        if(property.getParent() != null && property.getParent().getParent() != null){
            PrismObject<ShadowType> shadowPrism = (PrismObject<ShadowType>)property.getParent().getParent();

            Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadowPrism);
            if (attributes == null || attributes.isEmpty()){
            	return sb.toString();
            }

            //TODO - this is a dirty fix for situation, when attribute value is too long and it is a string without white chars,
            //thus it will not break in tooltip. break-all is also not good, since it can brake in the middle of words. What we
            //are doing here is replacing every, with ,&#8203;, &#8203; (the same with @) is a zero-width space, so the attribute value
            //will break after comma. This dirty fix will be removed when association editor is completed.
            for (ResourceAttribute<?> attr : attributes){
            	for (Object realValue : attr.getRealValues()){
            		sb.append(getAttributeName(attr));
                	sb.append(":");
                    if (realValue != null) {
                        sb.append(realValue.toString().replace(",", ",&#8203;").replace("@", "@&#8203;").replace("_", "@&#8203;"));
                    }
            		sb.append("<br>");
            	}
            }
        }

        return sb.toString();
    }

    private String getAttributeName(ResourceAttribute<?> attr) {
		if (attr.getDisplayName() != null){
			return attr.getDisplayName();
		}
		
		if (attr.getNativeAttributeName() != null){
			return attr.getNativeAttributeName();
		}
		
		if (attr.getElementName() != null){
			return attr.getElementName().getLocalPart();
		}
		
		return null; //TODO: is this ok?? or better is exception or some default name??
	}

	private void addValue(AjaxRequestTarget target) {
        Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
        ValueWrapper wrapper = model.getObject();
        ItemWrapper propertyWrapper = wrapper.getItem();
        LOGGER.debug("Adding value of {}", propertyWrapper);
        propertyWrapper.addValue();
        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

    private void removeValue(AjaxRequestTarget target) {
        ValueWrapper wrapper = model.getObject();
        ItemWrapper propertyWrapper = wrapper.getItem();
        LOGGER.debug("Removing value of {}", propertyWrapper);

        List<ValueWrapper> values = propertyWrapper.getValues();
        Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);

        switch (wrapper.getStatus()) {
            case ADDED:
                values.remove(wrapper);
                break;
            case DELETED:
                error("Couldn't delete already deleted item: " + wrapper.toString());
                target.add(((PageBase) getPage()).getFeedbackPanel());
            case NOT_CHANGED:
                if (inputPanel instanceof AssociationValueChoicePanel) {
                    ((PropertyWrapper)propertyWrapper).setStatus(ValueStatus.DELETED);
                }
                wrapper.setStatus(ValueStatus.DELETED);
                break;
        }

//        wrapper.getItem().getContainer().


        int count = countUsableValues(propertyWrapper);
        if (count == 0 && !hasEmptyPlaceholder(propertyWrapper)) {
            if (inputPanel instanceof ValueChoosePanel) {
                values.add(new ValueWrapper(propertyWrapper, new PrismReferenceValue(null), ValueStatus.ADDED));
            } else if (inputPanel instanceof AssociationValueChoicePanel) {
                Item item = propertyWrapper.getItem();
                ItemPath path = item.getPath();
                if (path != null){

                }
//                values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));
            } else {
                values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));
            }
        }
        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

    private ObjectQuery getAssociationsSearchQuery(PrismContext prismContext, PrismObject resource, QName objectClass, ShadowKindType kind,
                                                   String intent) {
        try {
            ObjectFilter andFilter = AndFilter.createAnd(
                    EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, objectClass),
                    EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, prismContext, kind),
//                    EqualFilter.createEqual(ShadowType.F_INTENT, ShadowType.class, prismContext, intent),
                    RefFilter.createReferenceEqual(new ItemPath(ShadowType.F_RESOURCE_REF), ShadowType.class, prismContext, resource.getOid()));
            ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);
            return query;
        } catch (SchemaException ex) {
            LoggingUtils.logException(LOGGER, "Unable to create associations search query", ex);
            return null;
        }

    }

}
