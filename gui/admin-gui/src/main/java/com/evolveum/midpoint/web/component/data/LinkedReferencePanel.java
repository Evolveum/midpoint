/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
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
        referenceModel = new LoadableModel<>() {

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
                    PrismObject<ObjectType> referencedObject = WebModelServiceUtils.resolveReferenceNoFetch(getModelObject(), getPageBase(),
                            task, result);
                    if (referencedObject != null) {
                        value.setObject(referencedObject.clone());
                    }
                }
                return value;
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);
        add(AttributeAppender.append("class", getAdditionalCssStyle()));

        IModel<DisplayType> displayModel = () -> {

            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return null;
            }

            DisplayType displayType = GuiDisplayTypeUtil.getDisplayTypeForObject(ref.getObject(), null, getPageBase());
            if (displayType == null) {
                displayType = new DisplayType();
            }
            if (displayType.getIcon() == null) {
                displayType.setIcon(IconAndStylesUtil.createIconType(IconAndStylesUtil.createDefaultBlackIcon(ref.getTargetType())));
            }
            return displayType;
        };

        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayModel);
        imagePanel.setRenderBodyOnly(true);
        add(imagePanel);

        AjaxLink<PrismReferenceValue> nameLink = new AjaxLink<>(ID_NAME, referenceModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                DetailsPageUtil.dispatchToObjectDetailsPage(referenceModel.getObject(), LinkedReferencePanel.this, false);
            }
        };
        nameLink.add(new EnableBehaviour(() -> {
            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return false;
            }

            return ref.getObject() != null;
        }));
        add(nameLink);

        Label nameLinkText = new Label(ID_NAME_TEXT, () -> {
            PrismReferenceValue ref = referenceModel.getObject();
            if (ref == null) {
                return "";
            }
            if (ref.getTargetName() == null && ref.getObject()== null) {
                return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(ref.asReferencable(), true, true);
            }
            return WebComponentUtil.getReferencedObjectDisplayNameAndName(ref.asReferencable(), false, getPageBase());
        });
        nameLinkText.setRenderBodyOnly(true);
        nameLink.add(nameLinkText);
    }

    public AjaxLink getLinkPanel() {
        return (AjaxLink) get(ID_NAME);
    }

    protected String getAdditionalCssStyle() {
        return "d-flex flex-wrap gap-2 align-items-center";
    }
}
