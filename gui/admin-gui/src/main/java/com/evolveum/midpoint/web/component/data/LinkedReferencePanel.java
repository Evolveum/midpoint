/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;


/**
 * Created by honchar
 */
public class LinkedReferencePanel<O extends ObjectType, R extends Referencable> extends BasePanel<R> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "nameLink";
    private static final String ID_NAME_TEXT = "nameLinkText";

    private static final String DOT_CLASS = LinkedReferencePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCED_OBJECT = DOT_CLASS + "loadReferencedObject";

    IModel<ObjectType> referencedObjectModel = null;

    public LinkedReferencePanel(String id, IModel<R> objectReferenceModel){
        super(id, objectReferenceModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initReferencedObjectModel();
        initLayout();
    }

    public void initReferencedObjectModel() {
            referencedObjectModel = new LoadableModel<ObjectType>() {
                @Override
                protected ObjectType load() {
                    if (getModelObject() == null || getModelObject().getType() == null){
                        return null;
                    }
                    if (getModelObject().asReferenceValue() != null && getModelObject().asReferenceValue().getObject() != null
                            && getModelObject().asReferenceValue().getObject().asObjectable() instanceof ObjectType ){
                        return (ObjectType)getModelObject().asReferenceValue().getObject().asObjectable();
                    }
                    if (StringUtils.isNotEmpty(getModelObject().getOid()) && getModelObject().getType() != null &&
                            getModelObject() instanceof ObjectReferenceType) {
                        PageBase pageBase = LinkedReferencePanel.this.getPageBase();
                        OperationResult result = new OperationResult(OPERATION_LOAD_REFERENCED_OBJECT);
                        PrismObject<ObjectType> referencedObject = WebModelServiceUtils.loadObject((ObjectReferenceType) getModelObject(), pageBase,
                                pageBase.createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT), result);
                        return referencedObject != null ? referencedObject.asObjectable() : null;
                    }
                    return null;
                }
            };
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DisplayType displayType = WebComponentUtil.getDisplayTypeForObject(referencedObjectModel.getObject(), null, getPageBase());
        if (displayType == null){
            displayType = new DisplayType();
        }
        if (displayType.getIcon() == null && referencedObjectModel.getObject() != null){
            displayType.setIcon(WebComponentUtil.createIconType(WebComponentUtil.createDefaultBlackIcon(
                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObjectModel.getObject().getClass()))));
        }
        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayType);
        imagePanel.setOutputMarkupId(true);
//        imagePanel.add(new VisibleBehaviour(() -> displayType != null && displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass())));
        add(imagePanel);

        AjaxLink<ObjectType> nameLink = new AjaxLink<ObjectType>(ID_NAME, referencedObjectModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (referencedObjectModel != null && referencedObjectModel.getObject() != null) {
                    WebComponentUtil.dispatchToObjectDetailsPage(referencedObjectModel.getObject().asPrismObject(),
                            LinkedReferencePanel.this);
                }
            }
        };
        nameLink.add(new EnableBehaviour(() ->
                referencedObjectModel != null && referencedObjectModel.getObject() != null
                && !new QName("dummy").getLocalPart().equals(
                        referencedObjectModel.getObject().asPrismContainer().getElementName().getLocalPart())));
        nameLink.setOutputMarkupId(true);
        add(nameLink);

        ObjectType referencedObject = referencedObjectModel.getObject();
        ObjectReferenceType referencedObjectRef = null;
        if (referencedObject != null) {
            referencedObjectRef = ObjectTypeUtil.createObjectRef(referencedObject.getOid(), referencedObject.getName(), ObjectTypes.getObjectType(referencedObject.getClass()));
            PrismReferenceValue referenceValue = getPageBase().getPrismContext().itemFactory().createReferenceValue(referencedObject.getOid(),
                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObject.getClass()));
            referenceValue.setObject(referencedObject.asPrismObject());
            referencedObjectRef.setupReferenceValue(referenceValue);
        }
        String nameLinkTextVal = (referencedObject instanceof UserType)?WebComponentUtil.getDisplayNameAndName(referencedObjectRef):
                WebComponentUtil.getDisplayNameOrName(referencedObjectRef);

        Label nameLinkText = new Label(ID_NAME_TEXT, Model.of(nameLinkTextVal));
        nameLinkText.setOutputMarkupId(true);
        nameLink.add(nameLinkText);

    }

}
