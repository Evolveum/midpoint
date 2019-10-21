/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;

/**
 * @author Viliam Repan (lazyman)
 * @author katkav
 */
public class MainPopupDialog extends ModalWindow {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_POPUP_BODY = "popupBody";

    private boolean initialized;

    public MainPopupDialog(String id) {
        super(id);
    }

//    @Override
//    protected void onInitialize() {
//        super.onInitialize();
//        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
//        showUnloadConfirmation(false);
//        setResizable(false);
//        setInitialWidth(350);
//        setInitialHeight(150);
//        setWidthUnit("px");
//
//        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
//                return true;
//            }
//        });
//
//        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClose(AjaxRequestTarget target) {
//                MainPopupDialog.this.close(target);
//            }
//        });
//
//        WebMarkupContainer content = new WebMarkupContainer(getContentId());
//        content.setOutputMarkupId(true);
//        content.setOutputMarkupPlaceholderTag(true);
//        setContent(content);
//
//        setOutputMarkupId(true);
//    }
//
//    @Override
//    protected void onBeforeRender(){
//        super.onBeforeRender();
//
//        if(initialized){
//            return;
//        }
//
//        Label initLabel = new Label(ID_MAIN_POPUP_BODY, new Model<>("Not initialized"));
//        initLabel.setOutputMarkupPlaceholderTag(true);
//        initLabel.setOutputMarkupId(true);
//        setBody(initLabel);
//       initialized = true;
//    }
//
//    private void setBody(Component component){
//        WebMarkupContainer content = (WebMarkupContainer) get(getContentId());
//        component.setOutputMarkupId(true);
//        component.setOutputMarkupPlaceholderTag(true);
//        content.setOutputMarkupPlaceholderTag(true);
//        content.setOutputMarkupId(true);
//        content.addOrReplace(component);
//    }
//
//    public void setBody(Popupable popupable){
//        setTitle(popupable.getTitle());
//        setInitialHeight(popupable.getHeight());
//        setInitialWidth(popupable.getWidth());
//        setHeightUnit(popupable.getHeightUnit());
//        setWidthUnit(popupable.getWidthUnit());
//        WebMarkupContainer content = (WebMarkupContainer) get(getContentId());
//        content.setOutputMarkupPlaceholderTag(true);
//        content.setOutputMarkupId(true);
//        Component component = popupable.getComponent();
//        component.setOutputMarkupId(true);
//        component.setOutputMarkupPlaceholderTag(true);
//        content.addOrReplace(component);
//    }




}
