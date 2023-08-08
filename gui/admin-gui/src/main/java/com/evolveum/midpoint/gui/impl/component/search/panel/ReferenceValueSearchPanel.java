/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author honchar
 */
public class ReferenceValueSearchPanel extends PopoverSearchPanel<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    private final PrismReferenceDefinition referenceDef;
    private static final String ID_SELECT_OBJECT_BUTTON = "selectObjectButton";

    public ReferenceValueSearchPanel(String id, IModel<ObjectReferenceType> model, PrismReferenceDefinition referenceDef) {
        super(id, model);
        this.referenceDef = referenceDef;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private  void initLayout(){
        setOutputMarkupId(true);
    }

    private <O extends ObjectType> void selectObjectPerformed(AjaxRequestTarget target) {
        List<QName> supportedTypes = getSupportedTargetList();
        if (CollectionUtils.isEmpty(supportedTypes)) {
            supportedTypes = ObjectTypeListUtil.createObjectTypeList();
        }
        ObjectBrowserPanel<O> objectBrowserPanel = new ObjectBrowserPanel<>(
                getPageBase().getMainPopupBodyId(), null, supportedTypes, false, getPageBase(),
                null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, O object) {
                getPageBase().hideMainPopup(target);
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setOid(object.getOid());
                ort.setTargetName(object.getName());
                ort.setType(object.asPrismObject().getComplexTypeDefinition().getTypeName());
                ReferenceValueSearchPanel.this.getModel().setObject(ort);
                referenceValueUpdated(ort, target);
                target.add(ReferenceValueSearchPanel.this);
            }
        };

        getPageBase().showMainPopup(objectBrowserPanel, target);
    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel(String id) {
        return new ReferenceValueSearchPopupPanel(id, ReferenceValueSearchPanel.this.getModel()) {

            private static final long serialVersionUID1 = 1L;

            @Override
            protected List<QName> getAllowedRelations() {
                return ReferenceValueSearchPanel.this.getAllowedRelations();
            }

            @Override
            protected List<QName> getSupportedTargetList() {
                return ReferenceValueSearchPanel.this.getSupportedTargetList();
            }

            @Override
            protected void confirmPerformed(AjaxRequestTarget target) {
                target.add(ReferenceValueSearchPanel.this);
                referenceValueUpdated(ReferenceValueSearchPanel.this.getModelObject(), target);
            }

            @Override
            protected Boolean isItemPanelEnabled() {
                return ReferenceValueSearchPanel.this.isItemPanelEnabled();
            }

            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return ReferenceValueSearchPanel.this.isAllowedNotFoundObjectRef();
            }

            @Override
            protected boolean isChooseObjectVisible() {
                return notDisplayedInPopup();
            }

            @Override
            protected void chooseObjectPerformed(AjaxRequestTarget target) {
                selectObjectPerformed(target);
            }
        };
    }

    @Override
    protected boolean isRemoveVisible() {
        return true;
    }

    @Override
    protected void removeSearchValue(AjaxRequestTarget target) {
        ObjectReferenceType ref = getModelObject();
        ref.asReferenceValue().setObject(null);
        ref.setOid(null);
        ref.setTargetName(null);
        ref.setRelation(null);

        target.add(this);
    }

    @Override
    protected LoadableModel<String> getTextValue() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                return WebComponentUtil.getReferenceObjectTextValue(getModelObject(), referenceDef, getPageBase());
            }
        };
    }

    protected List<QName> getSupportedTargetList() {
        if (referenceDef != null) {
            return WebComponentUtil.createSupportedTargetTypeList(referenceDef.getTargetTypeName());
        }
        return Collections.singletonList(ObjectType.COMPLEX_TYPE);
    }

    protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
    }

    protected List<QName> getAllowedRelations() {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, getPageBase());
    }

    protected boolean isAllowedNotFoundObjectRef(){
        return false;
    }

    private boolean notDisplayedInPopup() {
        return ReferenceValueSearchPanel.this.findParent(Popupable.class) == null;
    }
}
