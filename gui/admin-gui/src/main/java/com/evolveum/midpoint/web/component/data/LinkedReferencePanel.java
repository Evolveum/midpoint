/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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

//    IModel<ObjectType> referencedObjectModel = null;

    IModel<PrismReferenceValue> referenceModel;

    public LinkedReferencePanel(String id, IModel<R> objectReferenceModel) {
        super(id, objectReferenceModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initReferencedObjectModel();
        initLayout();
    }

    public void initReferencedObjectModel() {

        referenceModel = new LoadableModel<PrismReferenceValue>() {

            @Override
            protected PrismReferenceValue load() {
                if (getModelObject() == null || getModelObject().getOid() == null) {
                    return null;
                }
                PrismReferenceValue value = getModelObject().asReferenceValue().clone();
                if (value.getObject() == null) {
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT);
                    OperationResult result = task.getResult();
                    PrismObject<ObjectType> referencedObject = WebModelServiceUtils.loadObject(getModelObject(), getPageBase(),
                            task, result);
                    if (referencedObject != null) {
                        value.setObject(referencedObject.clone());
                    }
                }
                return value;
            }
        };

//             referencedObjectModel = new LoadableModel<ObjectType>() {
//                @Override
//                protected ObjectType load() {
//                    if (getModelObject() == null || getModelObject().getType() == null){
//                        return null;
//                    }
//                    if (getModelObject().asReferenceValue() != null && getModelObject().asReferenceValue().getObject() != null
//                            && getModelObject().asReferenceValue().getObject().asObjectable() instanceof ObjectType ){
//                        return (ObjectType)getModelObject().asReferenceValue().getObject().asObjectable();
//                    }
//                    if (StringUtils.isNotEmpty(getModelObject().getOid()) && getModelObject().getType() != null &&
//                            getModelObject() instanceof ObjectReferenceType) {
//                        PageBase pageBase = LinkedReferencePanel.this.getPageBase();
//                        OperationResult result = new OperationResult(OPERATION_LOAD_REFERENCED_OBJECT);
//                        PrismObject<ObjectType> referencedObject = WebModelServiceUtils.loadObject((ObjectReferenceType) getModelObject(), pageBase,
//                                pageBase.createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT), result);
//                        return referencedObject != null ? referencedObject.asObjectable() : null;
//                    }
//                    return null;
//                }
//            };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        IModel<DisplayType> displayModel = new ReadOnlyModel<>(() -> {

            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return null;
            }

            DisplayType displayType = WebComponentUtil.getDisplayTypeForObject(ref.getObject(), null, getPageBase());
            if (displayType == null) {
                displayType = new DisplayType();
            }
            if (displayType.getIcon() == null) {
                displayType.setIcon(WebComponentUtil.createIconType(WebComponentUtil.createDefaultBlackIcon(ref.getTargetType())));
            }
            return displayType;
        });

        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayModel);
        imagePanel.setOutputMarkupId(true);
//        imagePanel.add(new VisibleBehaviour(() -> displayType != null && displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass())));
        add(imagePanel);

        AjaxLink<PrismReferenceValue> nameLink = new AjaxLink<PrismReferenceValue>(ID_NAME, referenceModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                WebComponentUtil.dispatchToObjectDetailsPage(referenceModel.getObject(), LinkedReferencePanel.this, false);
            }
        };
        nameLink.add(new EnableBehaviour(() -> {
            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return false;
            }

            return ref.getObject() != null;
//                referencedObjectModel != null && referencedObjectModel.getObject() != null
//                && !new QName("dummy").getLocalPart().equals(
//                        referencedObjectModel.getObject().asPrismContainer().getElementName().getLocalPart())));
        }));
        nameLink.setOutputMarkupId(true);
        add(nameLink);

        Label nameLinkText = new Label(ID_NAME_TEXT, new ReadOnlyModel<>(() -> {
            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return "";
            }

            if (ref.getObject() != null) {
                return WebComponentUtil.getDisplayNameOrName(ref.getObject());
            }

            return WebComponentUtil.getDisplayNameOrName(ref.asReferencable());
        }));
        nameLinkText.setOutputMarkupId(true);
        nameLink.add(nameLinkText);

//        ObjectType referencedObject = referencedObjectModel.getObject();
//        ObjectReferenceType referencedObjectRef = null;
//        if (referencedObject != null) {
//            referencedObjectRef = ObjectTypeUtil.createObjectRef(referencedObject.getOid(), referencedObject.getName(), ObjectTypes.getObjectType(referencedObject.getClass()));
//            PrismReferenceValue referenceValue = getPageBase().getPrismContext().itemFactory().createReferenceValue(referencedObject.getOid(),
//                    WebComponentUtil.classToQName(getPageBase().getPrismContext(), referencedObject.getClass()));
//            referenceValue.setObject(referencedObject.asPrismObject());
//            referencedObjectRef.setupReferenceValue(referenceValue);
//        }
//        String nameLinkTextVal = (referencedObject instanceof UserType)?WebComponentUtil.getDisplayNameAndName(referencedObjectRef):
//                WebComponentUtil.getDisplayNameOrName(referencedObjectRef);

    }

}
