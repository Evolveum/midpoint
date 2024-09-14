/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class IconAjaxButtonBadgeNew extends BasePanel<RoleAnalysisAttributeAnalysisDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_BADGE = "badge";

    private static final String STATUS_ACTIVE = " active ";

//    boolean isClicked;
//    boolean isUnique = false;

    public IconAjaxButtonBadgeNew(String id, IModel<RoleAnalysisAttributeAnalysisDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
        addClickBehavior();
//        onLoadComponent();
    }

    private void initLayout() {
//        add(AttributeModifier.append(CLASS_CSS, getAdditionalCssClass()));

        add(AttributeModifier.replace(CLASS_CSS, () ->
                getModelObject().isSelected()
                ? getButtonCssClass() + STATUS_ACTIVE
                : getButtonCssClass()));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass()));
        image.setOutputMarkupId(true);
//        image.add(new Behavior() {
//            @Override
//            public void onConfigure(Component component) {
//                image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass()));
//                super.onConfigure(component);
//            }
//        });
        add(image);

        IModel<String> labelModel = createStringResource("${displayNameKey}", getModel());
        Label label = new Label(ID_TEXT, labelModel);
        label.add(AttributeModifier.append(TITLE_CSS, labelModel));
        label.add(new TooltipBehavior());
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.replace(CLASS_CSS, getLabelCssClass()));
        add(label);

        Label badge = new Label(ID_BADGE, getBadgeModel());
        badge.add(AttributeModifier.replace(CLASS_CSS, getBadgeCssClass()));
        badge.setOutputMarkupId(true);
        add(badge);
    }

    protected void onLoadComponent() {

    }


    private void addClickBehavior() {
        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
            }
        });
    }

    private String getButtonCssClass() {
        return "d-flex align-items-center gap-1 btn btn-sm btn-pill rounded-pill";
    }

    protected String getAdditionalCssClass() {
        return "d-flex align-items-center gap-2 ";
    }

    public IModel<String> getBadgeModel() {
        return () -> "(" + getModelObject().getAttributeValuesSize() + ")";
    }

    public String getIconCssClass() {
        if (getModelObject().isSelected()) {
            return "fa fa-check ml-1";
        } else {
            return UserType.class.equals(getModelObject().getType()) ? GuiStyleConstants.CLASS_OBJECT_USER_ICON : GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " ml-1";
        }

    }

//    public boolean isClicked() {
//        return isClicked;
//    }
//
//    public void setClicked(boolean clicked) {
//        isClicked = clicked;
//    }

    protected void onClick(AjaxRequestTarget target) {

    }
//        showAttributeStatistics = !showAttributeStatistics;
//        boolean tmp = isClicked;
//        isClicked = !tmp;
//
////                if (this.isClicked()) {
////                    if (classObjectIcon.contains("user")) {
////                        userPath.add(ItemPath.create(this.getModelObject()));
////                    } else {
////                        rolePath.add(ItemPath.create(this.getModelObject()));
////                    }
////                } else {
////                    if (classObjectIcon.contains("user")) {
////                        userPath.remove(this.getModelObject().toLowerCase());
////                    } else {
////                        rolePath.remove(this.getModelObject().toLowerCase());
////                    }
////                }
//
//        for (Component component : repeatingView) {
//            IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
//            if (!btn.isUnique) {
//                continue;
//            }
//            boolean clicked = btn.isClicked();
//            if (clicked && this.isClicked()) {
//                btn.setClicked(false);
//            } else if (!clicked && !this.isClicked()) { // && userPath.isEmpty() && rolePath.isEmpty()) {
//                btn.setClicked(true);
//            }
//
//            btn.add(AttributeModifier.replace(CLASS_CSS, btn.isClicked()
//                    ? getButtonCssClass() + STATUS_ACTIVE
//                    : getButtonCssClass()));
//
//            target.add(btn);
    //override in subclass
//    }

    protected String getBadgeCssClass() {
        return "ml-auto mr-1";
    }

    protected String getLabelCssClass() {
        return " pill-label";
    }



//    public boolean isUnique() {
//        return isUnique;
//    }
//
//    public void setUnique(boolean unique) {
//        isUnique = unique;
//    }
}
