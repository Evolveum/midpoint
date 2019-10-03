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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
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
public class IconedObjectNamePanel<AHT extends AssignmentHolderType> extends BasePanel<AHT> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "nameLink";
    private static final String ID_NAME_TEXT = "nameLinkText";

    private static final String DOT_CLASS = IconedObjectNamePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCED_OBJECT = DOT_CLASS + "loadReferencedObject";

    ObjectReferenceType objectReference;
    IModel<AHT> referencedObjectModel = null;

    public IconedObjectNamePanel(String id, ObjectReferenceType objectReference){
        super(id);
        this.objectReference = objectReference;
    }

    public IconedObjectNamePanel(String id, IModel<AHT> assignmentHolder){
        super(id, assignmentHolder);
        this.referencedObjectModel = assignmentHolder;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initReferencedObjectModel();
        initLayout();
    }

    public void initReferencedObjectModel() {
    	if (referencedObjectModel == null) {
    		referencedObjectModel = new LoadableModel<AHT>() {
                @Override
                protected AHT load() {
                    if (objectReference != null && objectReference.getType() != null &&
                            StringUtils.isNotEmpty(objectReference.getOid())) {
                    	PageBase pageBase = IconedObjectNamePanel.this.getPageBase();
                        OperationResult result = new OperationResult(OPERATION_LOAD_REFERENCED_OBJECT);
                        PrismObject<AHT> assignmentHolder = WebModelServiceUtils.loadObject(objectReference, pageBase,
                                pageBase.createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT), result);
                        return assignmentHolder != null ? assignmentHolder.asObjectable() : null;
                    } else {
                        return null;
                    }
                }
            };
    	}
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DisplayType displayType = WebComponentUtil.getArchetypePolicyDisplayType(referencedObjectModel.getObject(), getPageBase());
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

        AjaxLink<AHT> nameLink = new AjaxLink<AHT>(ID_NAME, referencedObjectModel) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (referencedObjectModel != null && referencedObjectModel.getObject() != null) {
                    WebComponentUtil.dispatchToObjectDetailsPage(referencedObjectModel.getObject().asPrismObject(),
                            IconedObjectNamePanel.this);
                }
            }
        };
        nameLink.add(new EnableBehaviour(() -> referencedObjectModel != null && referencedObjectModel.getObject() != null
                && !new QName("dummy").getLocalPart().equals(
                        referencedObjectModel.getObject().asPrismContainer().getElementName().getLocalPart())));
        nameLink.setOutputMarkupId(true);
        add(nameLink);

        Label nameLinkText = new Label(ID_NAME_TEXT, Model.of(WebComponentUtil.getEffectiveName(referencedObjectModel.getObject(),
                AbstractRoleType.F_DISPLAY_NAME)));
        nameLinkText.setOutputMarkupId(true);
        nameLink.add(nameLinkText);

    }

}
