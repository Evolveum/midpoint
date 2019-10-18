/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class MetadataPanel extends BasePanel<MetadataType>{

    private List<QName> metadataFieldsList = Arrays.asList(MetadataType.F_REQUEST_TIMESTAMP, MetadataType.F_REQUESTOR_REF,
            MetadataType.F_CREATE_TIMESTAMP, MetadataType.F_CREATOR_REF,
            MetadataType.F_CREATE_APPROVAL_TIMESTAMP, MetadataType.F_CREATE_APPROVER_REF,
            MetadataType.F_MODIFY_TIMESTAMP, MetadataType.F_MODIFIER_REF,
            MetadataType.F_MODIFY_APPROVAL_TIMESTAMP, MetadataType.F_MODIFY_APPROVER_REF);

    private static final String ID_METADATA_BLOCK = "metadataBlock";
    private static final String ID_METADATA_ROW = "metadataRow";
    private static final String ID_HEADER_CONTAINER = "headerContainer";
    private static final String ID_METADATA_PROPERTY_KEY = "metadataPropertyKey";
    private static final String ID_METADATA_FILED = "metadataField";
    private static final String ID_METADATA_LABEL = "metadataLabel";
    private static final String DOT_CLASS = MetadataPanel.class.getSimpleName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadObject";

    private String additionalHeaderStyle = "";
    private String header = "";

    public MetadataPanel(String id, IModel<MetadataType> model) {
        super(id, model);
        initLayout();
    }

    public MetadataPanel(String id, IModel<MetadataType> model, String header, String additionalHeaderStyle){
        super(id, model);
        this.additionalHeaderStyle = additionalHeaderStyle;
        this.header = header;
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer metadataBlock = new WebMarkupContainer(ID_METADATA_BLOCK);
        metadataBlock.setOutputMarkupId(true);
        add(metadataBlock);

        WebMarkupContainer headerContainer = new WebMarkupContainer(ID_HEADER_CONTAINER);
        headerContainer.setOutputMarkupId(true);
        headerContainer.add(new AttributeAppender("class", "prism-header " + additionalHeaderStyle));
        metadataBlock.add(headerContainer);

        Label metadataHeader = new Label(ID_METADATA_LABEL,
                createStringResource("AssignmentEditorPanel.metadataBlock", header != null ? header : ""));
        metadataHeader.setOutputMarkupId(true);
        headerContainer.add(metadataHeader);

        RepeatingView metadataRowRepeater = new RepeatingView(ID_METADATA_ROW);
        metadataBlock.add(metadataRowRepeater);
        for (QName qname : metadataFieldsList){
            WebMarkupContainer metadataRow = new WebMarkupContainer(metadataRowRepeater.newChildId());
            metadataRow.setOutputMarkupId(true);
            if (metadataFieldsList.indexOf(qname) % 2 != 0){
                metadataRow.add(new AttributeAppender("class", "stripe"));
            }
            metadataRowRepeater.add(metadataRow);

            metadataRow.add(new Label(ID_METADATA_PROPERTY_KEY, createStringResource(DOT_CLASS + qname.getLocalPart())));

            IModel<String> metadataFieldModel = new IModel<String>() {
                @Override
                public String getObject() {
                    PropertyModel<Object> tempModel = new PropertyModel<>(getModel(),
                        qname.getLocalPart());
                    if (tempModel.getObject() instanceof XMLGregorianCalendar){
                        return WebComponentUtil.getLongDateTimeFormattedValue((XMLGregorianCalendar)tempModel.getObject(), getPageBase());
                    } else if (tempModel.getObject() instanceof ObjectReferenceType){
                        ObjectReferenceType ref = (ObjectReferenceType) tempModel.getObject();
                        return WebComponentUtil.getName(ref, getPageBase(), OPERATION_LOAD_USER);
                    } else if (tempModel.getObject() instanceof List){
                        List list = (List) tempModel.getObject();
                        String result = "";
                        for (Object o : list){
                            if (o instanceof  ObjectReferenceType){
                                if (result.length() > 0){
                                    result += ", ";
                                }
                                result += WebComponentUtil.getName((ObjectReferenceType) o, getPageBase(), OPERATION_LOAD_USER);
                            }
                        }
                        return result;
                    }
                    return "";
                }
            };
            metadataRow.add(new Label(ID_METADATA_FILED, metadataFieldModel));
            metadataRow.add(new VisibleEnableBehaviour(){
                @Override
                public boolean isVisible(){
                    return StringUtils.isNotEmpty(metadataFieldModel.getObject());
                }
            });

        }


    }
}
