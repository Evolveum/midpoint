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
import org.apache.wicket.behavior.AttributeAppender;
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

    private static final String ID_LOCAL_PART = "localPart";
    private static final String ID_NAMESPACE = "namespace";
    private static final String ID_LOCAL_PART_LABEL = "localPartLabel";
    private static final String ID_NAMESPACE_LABEL = "namespaceLabel";
    private static final String ID_T_LOCAL_PART = "localPartTooltip";
    private static final String ID_T_NAMESPACE = "namespaceTooltip";

    public QNameEditorPanel(String id, IModel<QName> model){
        this(id, model, "QNameEditor.label.localPart", "QNameEditor.tooltip.localPart",
                "QNameEditor.label.namespace", "QNameEditor.tooltip.namespace");
    }

    public QNameEditorPanel(String id, IModel<QName> model, String localPartLabelKey, String localPartTooltipKey,
                            String namespaceLabelKey, String namespaceTooltipKey){
        super(id, model);

        initLayout(localPartLabelKey, localPartTooltipKey, namespaceLabelKey, namespaceTooltipKey);
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

    protected void initLayout(String localPartLabelKey, String localPartTooltipKey,
                              String namespaceLabelKey, String namespaceTooltipKey){

        Label localPartLabel = new Label(ID_LOCAL_PART_LABEL, getString(localPartLabelKey));
        localPartLabel.setOutputMarkupId(true);
        localPartLabel.setOutputMarkupPlaceholderTag(true);
        add(localPartLabel);

        Label namespaceLabel = new Label(ID_NAMESPACE_LABEL, getString(namespaceLabelKey));
        namespaceLabel.setOutputMarkupId(true);
        namespaceLabel.setOutputMarkupPlaceholderTag(true);
        add(namespaceLabel);

        TextField localPart = new TextField<>(ID_LOCAL_PART, new PropertyModel<String>(getModel(), "localPart"));
        localPart.setOutputMarkupId(true);
        localPart.setOutputMarkupPlaceholderTag(true);
        localPart.setRequired(isLocalPartRequired());
        add(localPart);

        DropDownChoice namespace = new DropDownChoice<>(ID_NAMESPACE, new PropertyModel<String>(getModel(), "namespaceURI"),
                prepareNamespaceList());
        namespace.setOutputMarkupId(true);
        namespace.setOutputMarkupPlaceholderTag(true);
        namespace.setNullValid(false);
        namespace.setRequired(true);
        add(namespace);

        Label localPartTooltip = new Label(ID_T_LOCAL_PART);
        localPartTooltip.add(new AttributeAppender("data-original-title", getString(localPartTooltipKey)));
        localPartTooltip.add(new InfoTooltipBehavior());
        localPartTooltip.setOutputMarkupPlaceholderTag(true);
        add(localPartTooltip);

        Label namespaceTooltip = new Label(ID_T_NAMESPACE);
        namespaceTooltip.add(new AttributeAppender("data-original-title", getString(namespaceTooltipKey)));
        namespaceTooltip.add(new InfoTooltipBehavior());
        namespaceTooltip.setOutputMarkupPlaceholderTag(true);
        add(namespaceTooltip);
    }

    /**
     *  Override to provide custom list of namespaces
     *  for QName editor
     * */
    protected List<String> prepareNamespaceList(){
        List<String> list = new ArrayList<>();

        //icfs
        list.add("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3");
        //ri
        list.add("http://midpoint.evolveum.com/xml/ns/public/resource/instance-3");

        return list;
    }

    /**
     *  Should localPart of QName be required?
     * */
    public boolean isLocalPartRequired(){
        return false;
    }
}
