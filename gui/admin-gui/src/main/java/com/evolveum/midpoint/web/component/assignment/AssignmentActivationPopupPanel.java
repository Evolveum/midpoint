/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Created by honchar.
 */
public class AssignmentActivationPopupPanel extends BasePanel<AssignmentEditorDto> implements Popupable{
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_ACTIVATION_BLOCK = "activationBlock";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_ADMIN_STATUS_CONTAINER = "administrativeStatusContainer";
    private static final String ID_VALID_FROM_CONTAINER = "validFromContainer";
    private static final String ID_VALID_TO_CONTAINER = "validToContainer";
    private static final String ID_OK_BUTTON = "okButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";


    public AssignmentActivationPopupPanel(String id, IModel<AssignmentEditorDto> assignmentModel){
        super(id, assignmentModel);
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer activationBlock = new WebMarkupContainer(ID_ACTIVATION_BLOCK);
        add(activationBlock);

        WebMarkupContainer adminStatusContainer = new WebMarkupContainer(ID_ADMIN_STATUS_CONTAINER);
        adminStatusContainer.setOutputMarkupId(true);
//        adminStatusContainer.add(new VisibleEnableBehaviour() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
//                        ActivationType.F_ADMINISTRATIVE_STATUS));
//            }
//        });
        activationBlock.add(adminStatusContainer);

        DropDownChoicePanel administrativeStatus = WebComponentUtil.createEnumPanel(
                ActivationStatusType.class, ID_ADMINISTRATIVE_STATUS,
                new PropertyModel<ActivationStatusType>(getModel(), AssignmentEditorDto.F_ACTIVATION + "."
                        + ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart()),
                this);
//        administrativeStatus.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isEnabled(){
//                return getModel().getObject().isEditable();
//            }
//        });
        adminStatusContainer.add(administrativeStatus);

        WebMarkupContainer validFromContainer = new WebMarkupContainer(ID_VALID_FROM_CONTAINER);
        validFromContainer.setOutputMarkupId(true);
//        validFromContainer.add(new VisibleEnableBehaviour() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
//                        ActivationType.F_VALID_FROM));
//            }
//        });
        activationBlock.add(validFromContainer);

        DateInput validFrom = new DateInput(ID_VALID_FROM,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
//        validFrom.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isEnabled(){
//                return getModel().getObject().isEditable();
//            }
//        });
        validFromContainer.add(validFrom);

        WebMarkupContainer validToContainer = new WebMarkupContainer(ID_VALID_TO_CONTAINER);
        validToContainer.setOutputMarkupId(true);
//        validToContainer.add(new VisibleEnableBehaviour() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
//                        ActivationType.F_VALID_TO));
//            }
//        });
        activationBlock.add(validToContainer);

        DateInput validTo = new DateInput(ID_VALID_TO,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
//        validTo.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isEnabled(){
//                return getModel().getObject().isEditable();
//            }
//        });
        validToContainer.add(validTo);

        activationBlock.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                // enabled activation in assignments for now.
                return true;
            }
        });

        AjaxButton okButton = new AjaxButton(ID_OK_BUTTON, createStringResource("AssignmentActivationPopupPanel.okButton")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
                reloadDateComponent(ajaxRequestTarget);
            }
        };
        add(okButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("AssignmentActivationPopupPanel.cancelButton")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        add(cancelButton);

        AssignmentsUtil.addAjaxOnUpdateBehavior((WebMarkupContainer)get(ID_ACTIVATION_BLOCK));

    }

    protected void reloadDateComponent(AjaxRequestTarget target){
    }

    public int getWidth(){
        return 600;
    }

    public int getHeight(){
        return 300;
    }

    public StringResourceModel getTitle(){
        return createStringResource("AssignmentActivationPopupPanel.title");
    }
    public Component getComponent(){
        return this;
    }
}
