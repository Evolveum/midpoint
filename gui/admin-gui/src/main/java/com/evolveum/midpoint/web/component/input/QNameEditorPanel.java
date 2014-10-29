/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class QNameEditorPanel extends SimplePanel<QName>{

    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_NAMESPACE = "namespace";
    private static final String ID_T_ATTRIBUTE = "attributeTooltip";
    private static final String ID_T_NAMESPACE = "namespaceTooltip";

    public QNameEditorPanel(String id, IModel<QName> model){
        super(id, model);
    }

    @Override
    public IModel<QName> getModel() {
        IModel<QName> model = super.getModel();
        QName modelObject = model.getObject();

        if(modelObject == null){
            model.setObject(new QName(""));
        }

        return model;
    }

    @Override
    protected void initLayout(){

        TextField attribute = new TextField<>(ID_ATTRIBUTE, new PropertyModel<String>(getModel(), "localPart"));
        attribute.setOutputMarkupId(true);
        attribute.setOutputMarkupPlaceholderTag(true);
        attribute.setRequired(true);
        add(attribute);

        DropDownChoice namespace = new DropDownChoice<>(ID_NAMESPACE, new PropertyModel<String>(getModel(), "namespaceURI"),
                prepareNamespaceList());
        namespace.setOutputMarkupId(true);
        namespace.setOutputMarkupPlaceholderTag(true);
        namespace.setNullValid(false);
        namespace.setRequired(true);
        add(namespace);

        Label attrTooltip = new Label(ID_T_ATTRIBUTE);
        attrTooltip.add(new InfoTooltipBehavior());
        attrTooltip.setOutputMarkupPlaceholderTag(true);
        add(attrTooltip);

        Label namespaceTooltip = new Label(ID_T_NAMESPACE);
        namespaceTooltip.add(new InfoTooltipBehavior());
        namespaceTooltip.setOutputMarkupPlaceholderTag(true);
        add(namespaceTooltip);
    }

    protected List<String> prepareNamespaceList(){
        List<String> list = new ArrayList<>();

        //icfs
        list.add("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3");
        //ri
        list.add("http://midpoint.evolveum.com/xml/ns/public/resource/instance-3");

        return list;
    }

//    public boolean isRequired(){
//        return false;
//    }
}
