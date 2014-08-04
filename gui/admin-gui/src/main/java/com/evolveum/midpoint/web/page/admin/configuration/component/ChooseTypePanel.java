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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 * */
public class ChooseTypePanel<T extends ObjectType> extends SimplePanel<ObjectViewDto>{

    private static final Trace LOGGER = TraceManager.getTrace(ChooseTypePanel.class);

    private static final String ID_OBJECT_NAME = "name";
    private static final String ID_LINK_CHOOSE = "choose";
    private static final String ID_LINK_REMOVE = "remove";

    private static final String MODAL_ID_SHOW_CHOOSE_OPTIONS = "showOptionsPopup";

    public ChooseTypePanel(String id, IModel<ObjectViewDto> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){

        final Label name = new Label(ID_OBJECT_NAME, new AbstractReadOnlyModel<String>(){

            @Override
            public String getObject(){
                ObjectViewDto dto = getModel().getObject();

                if(dto.getName() != null)
                    return getModel().getObject().getName();
                else
                    return createStringResource("chooseTypePanel.ObjectNameValue.null").getString();
            }
        });
        name.setOutputMarkupId(true);

        AjaxLink choose = new AjaxLink(ID_LINK_CHOOSE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                 changeOptionPerformed(target);
            }
        };

        AjaxLink remove = new AjaxLink(ID_LINK_REMOVE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setToDefault();
                target.add(name);
            }
        };

        add(choose);
        add(remove);
        add(name);

        initDialog();
    }

    private void initDialog(){
        ModalWindow dialog = new ChooseTypeDialog<T>(MODAL_ID_SHOW_CHOOSE_OPTIONS, getModel().getObject().getType()){

            @Override
            protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){
                choosePerformed(target, object);
            }

            @Override
            protected ObjectQuery getDataProviderQuery(){
                return getChooseQuery();
            }
        };

        add(dialog);
    }

    protected ObjectQuery getChooseQuery(){
        return null;
    }

    private void choosePerformed(AjaxRequestTarget target, ObjectType object){
        ModalWindow window = (ModalWindow) get(MODAL_ID_SHOW_CHOOSE_OPTIONS);
        window.close(target);

        ObjectViewDto o = getModel().getObject();

        o.setName(WebMiscUtil.getName(object));
        o.setOid(object.getOid());

        if(LOGGER.isTraceEnabled()){
            LOGGER.trace("Choose operation performed.");
        }

        target.add(get(ID_OBJECT_NAME));
    }

    private void changeOptionPerformed(AjaxRequestTarget target){
        ModalWindow window = (ModalWindow)get(MODAL_ID_SHOW_CHOOSE_OPTIONS);
        window.show(target);
    }

    private void setToDefault(){
        getModel().setObject(new ObjectViewDto());
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }
}
