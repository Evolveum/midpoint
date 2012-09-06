/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {

    private static final String MODAL_ID_BROWSER_TARGET = "browseTargetPopup";
    private static final String MODAL_ID_BROWSER_RESOURCE = "browseResourcePopup";

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_TARGET_OPTION = "targetOption";
    private static final String ID_CONSTRUCTION_OPTION = "constructionOption";

    private static final String ID_CONTAINERS = "containers";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";

    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_BROWSE_TARGET = "browseTarget";

    private static final String ID_RESOURCE_NAME = "resourceName";
    private static final String ID_BROWSE_RESOURCE = "browseResource";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_ACTIVATION = "activation";

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        add(description);

        RadioGroup radioGroup = new RadioGroup(ID_RADIO_GROUP);
        add(radioGroup);
        Radio targetOption = new Radio(ID_TARGET_OPTION);
        targetOption.add(new AjaxEventBehavior("onchange") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                showContainer(target, UserAssignmentDto.Type.TARGET);
            }
        });
        radioGroup.add(targetOption);
        Radio constructionOption = new Radio(ID_CONSTRUCTION_OPTION);
        constructionOption.add(new AjaxEventBehavior("onchange") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                showContainer(target, UserAssignmentDto.Type.ACCOUNT_CONSTRUCTION);
            }
        });
        radioGroup.add(constructionOption);

        WebMarkupContainer containers = new WebMarkupContainer(ID_CONTAINERS);
        containers.setOutputMarkupId(true);
        add(containers);

        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        targetContainer.add(createContainerVisibleBehaviour(UserAssignmentDto.Type.TARGET));
        containers.add(targetContainer);

        initTargetContainer(targetContainer);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        constructionContainer.add(createContainerVisibleBehaviour(UserAssignmentDto.Type.ACCOUNT_CONSTRUCTION));
        containers.add(constructionContainer);

        initConstructionContainer(constructionContainer);

        //todo extension and activation
//        TextArea extension = new TextArea(ID_EXTENSION, new PropertyModel(model, AssignmentEditorDto.F_EXTENSION));
//        assignmentForm.add(extension);

        ModalWindow targetDialog = createDialog(MODAL_ID_BROWSER_TARGET);
//        targetDialog.setContent() //todo implement panel
        add(targetDialog);

        final ModalWindow resourceDialog = createDialog(MODAL_ID_BROWSER_RESOURCE);
        resourceDialog.setContent(new ResourceListPanel(resourceDialog.getContentId()) {

            @Override
            public void resourceSelectedPerformed(AjaxRequestTarget target, ResourceType resource) {
                resourceDialog.close(target);
                AssignmentEditorPanel.this.resourceSelectedPerformed(target, resource);
            }
        });
        add(resourceDialog);
    }

    private ModalWindow createDialog(String id) {
        final ModalWindow modal = new ModalWindow(id);
        add(modal);

        modal.setResizable(false);
        modal.setTitle(getString("AssignmentEditorPanel.browser.title"));
        modal.setCookieName(AssignmentEditorPanel.class.getSimpleName() + ((int) (Math.random() * 100)));

        modal.setInitialWidth(1100);
        modal.setWidthUnit("px");

        modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                modal.close(target);
            }
        });

        modal.add(new AbstractAjaxBehavior() {
            @Override
            public void onRequest() {
            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                response.renderOnDomReadyJavaScript("Wicket.Window.unloadConfirmation = false;");
            }
        });

        return modal;
    }

    private VisibleEnableBehaviour createContainerVisibleBehaviour(final UserAssignmentDto.Type type) {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                if (type.equals(dto.getType())) {
                    return true;
                }

                return false;
            }
        };
    }

    private void initConstructionContainer(WebMarkupContainer constructionContainer) {
        Label resourceName = new Label(ID_RESOURCE_NAME, createResourceNameModel());
        resourceName.setOutputMarkupId(true);
        constructionContainer.add(resourceName);

        AjaxLinkButton browseResource = new AjaxLinkButton(ID_BROWSE_RESOURCE,
                createStringResource("AssignmentEditorPanel.button.browse")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseResourcePerformed(target);
            }
        };
        constructionContainer.add(browseResource);

        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        constructionContainer.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE,
                new LoadableModel<List<ACAttributeDto>>(false) {

                    @Override
                    protected List<ACAttributeDto> load() {
                        return loadAttributes();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, listItem.getModel());
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
            }
        };
        attributes.add(attribute);
    }

    private void initTargetContainer(WebMarkupContainer targetContainer) {
        Label targetName = new Label(ID_TARGET_NAME); //todo model
        targetName.setOutputMarkupId(true);
        targetContainer.add(targetName);

        AjaxLinkButton browseResource = new AjaxLinkButton(ID_BROWSE_TARGET,
                createStringResource("AssignmentEditorPanel.button.browse")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseTargetPerformed(target);
            }
        };
        targetContainer.add(browseResource);
    }

    private List<ACAttributeDto> loadAttributes() {
        //todo implement
        return new ArrayList<ACAttributeDto>();
    }

    private void browseTargetPerformed(AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_BROWSER_TARGET);
        window.show(target);
    }

    private void browseResourcePerformed(AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_BROWSER_RESOURCE);
        window.show(target);
    }

    private void resourceSelectedPerformed(AjaxRequestTarget target, ResourceType resource) {

        //todo implement
    }

    private void showContainer(AjaxRequestTarget target, UserAssignmentDto.Type type) {
        //todo implement
        getModel().getObject().setType(type);
        target.add(get(ID_CONTAINERS));
    }

    private IModel createResourceNameModel() {
        return new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                AssignmentEditorDto dto = getModel().getObject();
                PrismObject<ResourceType> resource = dto.getResource();
                if (resource == null) {
                    return null;
                }
                return WebMiscUtil.getName(resource);
            }
        };
    }
}
