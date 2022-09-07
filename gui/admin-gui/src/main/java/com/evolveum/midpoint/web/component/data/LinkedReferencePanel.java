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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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
public class LinkedReferencePanel<R extends Referencable> extends BasePanel<R> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "nameLink";
    private static final String ID_NAME_TEXT = "nameLinkText";

    private static final String DOT_CLASS = LinkedReferencePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCED_OBJECT = DOT_CLASS + "loadReferencedObject";

    IModel<PrismReferenceValue> referenceModel = null;

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
        referenceModel = new LoadableModel<PrismReferenceValue>() {

            @Override
            protected PrismReferenceValue load() {
                R ref = getModelObject();
                if (ref == null) {
                    return null;
                }

                PrismReferenceValue value = ref.asReferenceValue().clone();
                if (value.getOid() == null && value.getObject() == null) {
                    return null;
                }

                if (value.getObject() == null) {
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT);
                    OperationResult result = task.getResult();
                    PrismObject<ObjectType> referencedObject = WebModelServiceUtils.loadObject(ref, getPageBase(), task, result);
                    if (referencedObject != null) {
                        value.setObject(referencedObject.clone());
                    }
                }
                return value;
            }
        };
    }

    private void initLayout(){
        setOutputMarkupId(true);

        PrismReferenceValue ref = referenceModel.getObject();
        DisplayType displayType;
        if (ref == null) {
            displayType = new DisplayType();
        } else {
            displayType = WebComponentUtil.getDisplayTypeForObject(
                    ((PrismObject<ObjectType>)ref.getObject()).asObjectable(), null, getPageBase());

            if (displayType == null){
                displayType = new DisplayType();
            }
            if (displayType.getIcon() == null && ref.getObject() != null){
                displayType.setIcon(WebComponentUtil.createIconType(WebComponentUtil.createDefaultBlackIcon(
                        WebComponentUtil.classToQName(getPageBase().getPrismContext(),
                                ((PrismObject<ObjectType>)ref.getObject()).asObjectable().getClass()))));
            }
        }

        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayType);
        imagePanel.setOutputMarkupId(true);
//        imagePanel.add(new VisibleBehaviour(() -> displayType != null && displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass())));
        add(imagePanel);

        AjaxLink<PrismReferenceValue> nameLink = new AjaxLink<PrismReferenceValue>(ID_NAME, referenceModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (referenceModel != null && referenceModel.getObject() != null) {
                    WebComponentUtil.dispatchToObjectDetailsPage(referenceModel.getObject(),
                            LinkedReferencePanel.this, false);
                }
            }
        };
        nameLink.add(new EnableBehaviour(() -> ref != null && ref.getOid() != null));
        nameLink.setOutputMarkupId(true);
        add(nameLink);

        ObjectType referencedObject = null;
        ObjectReferenceType referencedObjectRef = null;
        if (ref != null && ref.getObject() != null && ref.getObject().asObjectable() != null) {
            referencedObject = ((ObjectType) ref.getObject().asObjectable());
            referencedObjectRef = WebComponentUtil.createObjectRef(
                    referencedObject.getOid(), referencedObject.getName() != null ? referencedObject.getName().getOrig() : null,
                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObject.getClass()));
            PrismReferenceValue referenceValue = getPageBase().getPrismContext().itemFactory().createReferenceValue(referencedObject.getOid(),
                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObject.getClass()));
            referenceValue.setObject(referencedObject.asPrismObject());
            referencedObjectRef.setupReferenceValue(referenceValue);
        }
        String nameLinkTextVal = (referencedObject instanceof UserType) ? WebComponentUtil.getDisplayNameAndName(referencedObjectRef):
                WebComponentUtil.getDisplayNameOrName(referencedObjectRef);
        if (StringUtils.isEmpty(nameLinkTextVal) && referencedObjectRef != null && StringUtils.isNotEmpty(referencedObjectRef.getOid())) {
            nameLinkTextVal = referencedObjectRef.getOid();
        }

        Label nameLinkText = new Label(ID_NAME_TEXT, Model.of(nameLinkTextVal));
        nameLinkText.setOutputMarkupId(true);
        nameLink.add(nameLinkText);

    }

}
