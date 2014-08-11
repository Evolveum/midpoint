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

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ObjectPolicyDialog;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.Iterator;
import java.util.List;

/**
 *  @author shood
 */

public class ObjectPolicyConfigurationEditor extends SimplePanel<List<ObjectPolicyConfigurationTypeDto>> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyConfigurationEditor.class);

    private static final String DOT_CLASS = ObjectPolicyConfigurationEditor.class.getName() + ".";

    private static final String OPERATION_LOAD_OBJECT_TEMPLATE = DOT_CLASS + "loadObjectTemplate";

    private static final String ID_MODAL_WINDOW = "templateConfigModal";

    private static final String ID_LABEL = "label";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_BUTTON_EDIT = "edit";
    private static final String ID_NAME = "name";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";
    private static final String OFFSET_CLASS = "col-md-offset-4";

    public ObjectPolicyConfigurationEditor(String id, IModel<List<ObjectPolicyConfigurationTypeDto>> model){
        super(id, model);

        setOutputMarkupId(true);
    }

    @Override
    protected void initLayout(){

        final Label label = new Label(ID_LABEL, createStringResource("objectPolicyConfigurationEditor.label"));
        add(label);

        ListView repeater = new ListView<ObjectPolicyConfigurationTypeDto>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem item) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                textWrapper.add(AttributeAppender.prepend("class", new AbstractReadOnlyModel<String>(){

                    @Override
                    public String getObject(){
                        if(item.getIndex() > 0){
                            return OFFSET_CLASS + " " + CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(textWrapper);

                TextField name = new TextField<>(ID_NAME, createNameModel(item.getModel()));
                name.setOutputMarkupId(true);
                name.add(new AjaxFormComponentUpdatingBehavior("onblur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {}
                });
                name.setEnabled(false);
                name.add(AttributeAppender.replace("placeholder", createStringResource("objectPolicyConfigurationEditor.name.placeholder")));
                textWrapper.add(name);

                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(name));
                textWrapper.add(feedback);

                AjaxLink edit = new AjaxLink(ID_BUTTON_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editPerformed(target, item);
                    }
                };
                textWrapper.add(edit);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                buttonGroup.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

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

        initDialog();
        repeater.setOutputMarkupId(true);
        add(repeater);
    }

    private void initDialog(){
        ModalWindow editor = new ObjectPolicyDialog(ID_MODAL_WINDOW, null){

            @Override
            protected void savePerformed(AjaxRequestTarget target){
                ObjectPolicyConfigurationTypeDto oldConfig = getModel().getObject().getConfig();
                ObjectPolicyConfigurationTypeDto newConfig = getModel().getObject().preparePolicyConfig();

                ObjectPolicyConfigurationEditor.this.replace(oldConfig, newConfig);

                target.add(ObjectPolicyConfigurationEditor.this);
                close(target);
            }
        };
        add(editor);
    }

    private void replace(ObjectPolicyConfigurationTypeDto old, ObjectPolicyConfigurationTypeDto newC){
        boolean added = false;

        List<ObjectPolicyConfigurationTypeDto> list = getModelObject();
        for(ObjectPolicyConfigurationTypeDto o: list){
            if(old.equals(o)){
                o.setConstraints(newC.getConstraints());
                o.setTemplateRef(newC.getTemplateRef());
                o.setType(newC.getType());
                added = true;
            }
        }

        if(!added){
            list.add(newC);
        }
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem item){
        AjaxLink add = new AjaxLink(ID_BUTTON_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible(item);
            }
        });
        buttonGroup.add(add);

        AjaxLink remove = new AjaxLink(ID_BUTTON_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target, item);
            }
        };
        remove.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        buttonGroup.add(remove);
    }

    protected boolean isAddButtonVisible(ListItem item) {
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

    private IModel<String> createNameModel(final IModel<ObjectPolicyConfigurationTypeDto> model){
        return new LoadableModel<String>() {

            @Override
            public String load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_TEMPLATE);
                StringBuilder sb = new StringBuilder();
                ObjectPolicyConfigurationTypeDto config = model.getObject();

                if(config != null){
                    ObjectReferenceType ref = config.getTemplateRef();

                    if(ref != null){
                        String oid = ref.getOid();

                        PrismObject<ObjectTemplateType> template = WebModelUtils.loadObject(ObjectTemplateType.class, oid, result, getPageBase());

                        if(template != null){
                            ObjectTemplateType tmp = template.asObjectable();
                            sb.append(WebMiscUtil.getOrigStringFromPoly(tmp.getName())).append(": ");
                        }
                    }

                    if(config.getType() != null){
                        sb.append(config.getType().getLocalPart());
                    }
                }

                return sb.toString();
            }
        };
    }

    private void addPerformed(AjaxRequestTarget target){
        List<ObjectPolicyConfigurationTypeDto> list = getModelObject();
        list.add(new ObjectPolicyConfigurationTypeDto());

        target.add(this);
    }

    private void removePerformed(AjaxRequestTarget target, ListItem item){
        List<ObjectPolicyConfigurationTypeDto> list = getModelObject();
        Iterator<ObjectPolicyConfigurationTypeDto> iterator = list.iterator();

        while (iterator.hasNext()){
            ObjectPolicyConfigurationTypeDto object = iterator.next();

            if(object.equals(item.getModelObject())){
                iterator.remove();
                break;
            }
        }

        if(list.size() == 0){
            list.add(new ObjectPolicyConfigurationTypeDto());
        }

        target.add(this);
    }

    private void editPerformed(AjaxRequestTarget target, ListItem item){
        ObjectPolicyDialog window = (ObjectPolicyDialog) get(ID_MODAL_WINDOW);
        window.updateModel(target, (ObjectPolicyConfigurationTypeDto)item.getModelObject());
        window.show(target);
    }

}
