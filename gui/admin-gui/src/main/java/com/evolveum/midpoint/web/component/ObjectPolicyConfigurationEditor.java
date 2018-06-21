/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ObjectPolicyPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;

/**
 *  @author shood
 */

public class ObjectPolicyConfigurationEditor extends BasePanel<List<ObjectPolicyConfigurationType>> {

	private static final long serialVersionUID = 1L;


	private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyConfigurationEditor.class);

	private static final String DOT_CLASS = ObjectPolicyConfigurationEditor.class.getName() + ".";

    private static final String ID_LABEL = "label";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_BUTTON_EDIT = "edit";
    private static final String ID_NAME = "name";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_BUTTON_ADD_FIRST = "addFirstButton";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";
    private static final String OFFSET_CLASS = "col-md-offset-4";

    public ObjectPolicyConfigurationEditor(String id, IModel<List<ObjectPolicyConfigurationType>> model){
        super(id, model);

        setOutputMarkupId(true);

        initLayout();
    }

    protected void initLayout(){

        final Label label = new Label(ID_LABEL, createStringResource("objectPolicyConfigurationEditor.label"));
        add(label);

        AjaxLink<String> addFirst = new AjaxLink<String>(ID_BUTTON_ADD_FIRST) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        addFirst.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                List<ObjectPolicyConfigurationType> modelObj = ObjectPolicyConfigurationEditor.this.getModelObject();
                return modelObj == null || modelObj.size() == 0;
            }
        });
        addFirst.setOutputMarkupId(true);
        add(addFirst);

        ListView<ObjectPolicyConfigurationType> repeater = new ListView<ObjectPolicyConfigurationType>(ID_REPEATER, getModel()) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ObjectPolicyConfigurationType> item) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                textWrapper.add(AttributeAppender.prepend("class", new AbstractReadOnlyModel<String>(){
					private static final long serialVersionUID = 1L;

					@Override
                    public String getObject(){
                        if(item.getIndex() > 0){
                            return OFFSET_CLASS + " " + CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(textWrapper);

                TextField<String> name = new TextField<>(ID_NAME, createNameModel(item.getModel()));
                name.setOutputMarkupId(true);
                name.add(new AjaxFormComponentUpdatingBehavior("blur") {
                	private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {}
                });
                name.setEnabled(false);
                name.add(AttributeAppender.replace("placeholder", createStringResource("objectPolicyConfigurationEditor.name.placeholder")));
                textWrapper.add(name);

                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(name));
                textWrapper.add(feedback);

                AjaxLink<String> edit = new AjaxLink<String>(ID_BUTTON_EDIT) {
                	private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editPerformed(target, item);
                    }
                };
                textWrapper.add(edit);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                buttonGroup.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {
                	private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if(item.getIndex() > 0){
                            return CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(buttonGroup);
                initButtons(buttonGroup, item);
            }
        };

        repeater.setOutputMarkupId(true);
        add(repeater);
    }

    private void replace(ObjectPolicyConfigurationType old, ObjectPolicyConfigurationType newC){
        boolean added = false;

        List<ObjectPolicyConfigurationType> list = getModelObject();
        for(ObjectPolicyConfigurationType o: list){
            if(old.equals(o)){
            		o.getPropertyConstraint().clear();
            		for (PropertyConstraintType constraintType : newC.getPropertyConstraint()) {
            			PrismContainerValue<PropertyConstraintType> constraint = constraintType.asPrismContainerValue();
            			o.getPropertyConstraint().add(constraint.clone().asContainerable());
            		}
            		o.setObjectTemplateRef(newC.getObjectTemplateRef());
                o.setType(newC.getType());
                o.setSubtype(newC.getSubtype());
                added = true;
            }
        }

        if(!added){
            list.add((ObjectPolicyConfigurationType) newC.asPrismContainerValue().clone().asContainerable());
        }
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<ObjectPolicyConfigurationType> item){
        AjaxLink<String> add = new AjaxLink<String>(ID_BUTTON_ADD) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isAddButtonVisible(item);
            }
        });
        buttonGroup.add(add);

        AjaxLink<String> remove = new AjaxLink<String>(ID_BUTTON_REMOVE) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target, item);
            }
        };
        remove.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        buttonGroup.add(remove);
    }

    protected boolean isAddButtonVisible(ListItem<ObjectPolicyConfigurationType> item) {
       int size = getModelObject().size();
        if (size <= 1) {
            return true;
        }
        if (item.getIndex() == size - 1) {
            return true;
        }

        return false;
    }

    protected boolean isRemoveButtonVisible() {
        int size = getModelObject().size();
        if (size > 0) {
            return true;
        }

        return false;
    }

    private IModel<String> createNameModel(final IModel<ObjectPolicyConfigurationType> model){
        return new LoadableModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String load() {
                StringBuilder sb = new StringBuilder();
                ObjectPolicyConfigurationType config = model.getObject();

                if(config != null){
                    ObjectReferenceType ref = config.getObjectTemplateRef();

                    if (ref != null) {
                    	sb.append(WebComponentUtil.getOrigStringFromPoly(ref.getTargetName()));
                    }

                    if (config.getConflictResolution() != null) {
                    	if (sb.length() > 0) {
                    		sb.append(" ");
	                    }
                        sb.append(getString("ObjectPolicyConfigurationEditor.conflictResolution"));
                    }

                    if(config.getType() != null) {
                    	if (sb.length() > 0) {
		                    sb.append(": ");
	                    }
                        sb.append(config.getType().getLocalPart());
                    }

                    if (config.getSubtype() != null) {
                    	sb.append("(").append(config.getSubtype()).append(")");
                    }
                }

                return sb.toString();
            }
        };
    }

    private void addPerformed(AjaxRequestTarget target){
        List<ObjectPolicyConfigurationType> list = getModelObject();
        list.add(new ObjectPolicyConfigurationType());

        target.add(this);
    }

    private void removePerformed(AjaxRequestTarget target, ListItem<ObjectPolicyConfigurationType> item){
        List<ObjectPolicyConfigurationType> list = getModelObject();
        Iterator<ObjectPolicyConfigurationType> iterator = list.iterator();

        while (iterator.hasNext()){
            ObjectPolicyConfigurationType object = iterator.next();

            if(object.equals(item.getModelObject())){
                iterator.remove();
                break;
            }
        }

        target.add(this);
    }

    private void editPerformed(AjaxRequestTarget target, ListItem<ObjectPolicyConfigurationType> item){
    	ObjectPolicyPanel objectPolicyPanel = new ObjectPolicyPanel(getPageBase().getMainPopupBodyId(), item.getModelObject()) {
    		private static final long serialVersionUID = 1L;

			@Override
             protected void savePerformed(AjaxRequestTarget target){
                 ObjectPolicyConfigurationType oldConfig = getModel().getObject().getConfig();
                 OperationResult result = new OperationResult(DOT_CLASS + "preparePropertyConstraint");
                 ObjectPolicyConfigurationType newConfig = getModel().getObject().preparePolicyConfig(result);
//
                 ObjectPolicyConfigurationEditor.this.replace(oldConfig, newConfig);
                 ObjectPolicyConfigurationEditor.this.getPageBase().hideMainPopup(target);
                 getPageBase().showResult(result, false);
                 target.add(ObjectPolicyConfigurationEditor.this);
             }
    	};
    	objectPolicyPanel.setOutputMarkupId(true);
    	getPageBase().showMainPopup(objectPolicyPanel, target);
    }

}
