/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public abstract class FocusSummaryPanel<O extends ObjectType> extends ObjectSummaryPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = FocusSummaryPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PARENT_ORGS = DOT_CLASS + "activationTag";


    public FocusSummaryPanel(String id, Class<O> type, final IModel<O> model, ModelServiceLocator serviceLocator) {
        super(id, type, model, serviceLocator);
    }

    @Override
    protected List<SummaryTag<O>> getSummaryTagComponentList(){
        List<SummaryTag<O>> summaryTagList = new ArrayList<>();

        SummaryTag<O> tagActivation = new SummaryTag<O>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(O object) {
                ActivationType activation = null;
//                O object = object.asObjectable();
                if (object instanceof FocusType) {
                    activation = ((FocusType)object).getActivation();
                }
                if (activation == null) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
                    setLabel(getString("ActivationStatusType.ENABLED"));

                } else if (activation.getEffectiveStatus() == ActivationStatusType.DISABLED) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
                    setLabel(getString("ActivationStatusType.DISABLED"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);

                } else if (activation.getEffectiveStatus() == ActivationStatusType.ARCHIVED) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
                    setLabel(getString("ActivationStatusType.ARCHIVED"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_ARCHIVED);

                } else {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
                    setLabel(getString("ActivationStatusType.ENABLED"));
                }
            }
        };
        tagActivation.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isActivationVisible();
            }
        });
        summaryTagList.add(tagActivation);
        return summaryTagList;
    }

    @Override
    protected IModel<String> getDefaltParentOrgModel() {
        return new ReadOnlyModel<>(() -> {
            O focusObject = FocusSummaryPanel.this.getModel().getObject();
            List<OrgType> parentOrgs = focusObject != null ? WebComponentUtil.loadReferencedObjectList(focusObject.getParentOrgRef(),
                    OPERATION_LOAD_PARENT_ORGS, FocusSummaryPanel.this.getPageBase()) : null;
            if (parentOrgs == null || parentOrgs.isEmpty()) {
                return "";
            }
            // Kinda hack now .. "functional" orgType always has preference
            // this whole thing should be driven by an expression later on
            for (OrgType orgType : parentOrgs) {
                if (FocusTypeUtil.determineSubTypes(orgType).contains("functional")) {
                    return WebComponentUtil.getDisplayNameOrName(orgType.asPrismObject());
                }
            }
            //search for manager org at first
            for (ObjectReferenceType orgRef : focusObject.getParentOrgRef()) {
                if (orgRef.getRelation() != null && RelationTypes.MANAGER.equals(orgRef.getRelation())) {
                    for (OrgType orgType : parentOrgs){
                        if (orgType.getOid().equals(orgRef.getOid())){
                            return WebComponentUtil.getDisplayNameOrName(orgType.asPrismObject());
                        }
                    }
                }
            }
            // Just use the first one as a fallback
            return WebComponentUtil.getDisplayNameOrName(parentOrgs.iterator().next().asPrismObject());
        });
    }

    @Override
    protected void addAdditionalExpressionVariables(ExpressionVariables variables) {
        List<OrgType> parentOrgs = new ArrayList<>();
        for (ObjectReferenceType parentOrgRef : getModelObject().getParentOrgRef()) {
            if (parentOrgRef != null && parentOrgRef.asReferenceValue().getObject() != null) {
                parentOrgs.add((OrgType) parentOrgRef.asReferenceValue().getObject().asObjectable());
            }
        }
        variables.putList(ExpressionConstants.VAR_ORGS, parentOrgs);
    }

    @Override
    protected IModel<AbstractResource> getPhotoModel() {
        return new IModel<AbstractResource>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AbstractResource getObject() {
                byte[] jpegPhoto = null;
                O object = getModel().getObject();
                if (object == null){
                    return null;
                }
                if (object instanceof FocusType) {
                    jpegPhoto = ((FocusType) object).getJpegPhoto();
                }
                if (jpegPhoto == null) {
                    return null;
                } else {
                    return new ByteArrayResource("image/jpeg", jpegPhoto);
                }
            }
        };
    }

    protected boolean isActivationVisible() {
        return true;
    }

    public static void addSummaryPanel(MarkupContainer parentComponent, PrismObject<FocusType> focus, PrismObjectWrapper<FocusType> focusWrapper, String id, ModelServiceLocator serviceLocator) {
        if (focus.getCompileTimeClass().equals(UserType.class)) {
            parentComponent.add(new UserSummaryPanel(id,
                    Model.of((UserType)focus.asObjectable()), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(RoleType.class)) {
            parentComponent.add(new RoleSummaryPanel(id,
                    Model.of((RoleType)focus.asObjectable()), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(OrgType.class)) {
            parentComponent.add(new OrgSummaryPanel(id,
                    Model.of((OrgType)focus.asObjectable()), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(ServiceType.class)) {
            parentComponent.add(new ServiceSummaryPanel(id,
                    Model.of((ServiceType)focus.asObjectable()), serviceLocator));
        }
    }
}
