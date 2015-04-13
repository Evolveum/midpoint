/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismObjectPanel extends Panel {

    private static final String STRIPED_CLASS = "striped";
    private static final String ID_HEADER = "header";

    private boolean showHeader = true;
    private PageBase pageBase;

    public PrismObjectPanel(String id, IModel<ObjectWrapper> model, ResourceReference image, Form form, PageBase pageBase) {
        super(id);
        setOutputMarkupId(true);

        this.pageBase = pageBase;
        initLayout(model, image, form);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(PrismObjectPanel.class, "PrismObjectPanel.css")));

        StringBuilder sb = new StringBuilder();
        sb.append("fixStripingOnPrismForm('").append(getMarkupId()).append("', '").append(STRIPED_CLASS).append("');");
        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    private AjaxEventBehavior createHeaderOnClickBehaviour(final IModel<ObjectWrapper> model) {
        return new AjaxEventBehavior("onClick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        };
    }

    private IModel<String> createHeaderClassModel(final IModel<ObjectWrapper> model) {
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isProtectedAccount()) {
                    return "protected";
                }

                return wrapper.getHeaderStatus().name().toLowerCase();
            }
        };
    }

    private IModel<String> createHeaderNameClassModel(final IModel<ObjectWrapper> model) {
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (isDisabled(wrapper)) {
                    return "disable";
                }
                return "";
            }
        };
    }

    /**
     * Method uses value from administrativeStatus property for
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType}
     * and effectiveStatus property for {@link com.evolveum.midpoint.xml.ns._public.common.common_3.UserType}.
     *
     * @return true if panel should look like its' object is disabled (strike through font).
     */
    public boolean isDisabled(ObjectWrapper wrapper) {
//        PrismObject object = wrapper.getObject();
//
//        if (UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
//            ActivationStatusType status = getActivationStatus(object, ActivationType.F_EFFECTIVE_STATUS);
//            return ActivationStatusType.DISABLED.equals(status);
//        } else if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())) {
//            ActivationStatusType status = getActivationStatus(object, ActivationType.F_ADMINISTRATIVE_STATUS);
//            return ActivationStatusType.DISABLED.equals(status);
//        }

        // attempt to fix MID-1580
        ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(
                ShadowType.F_ACTIVATION));
        if (activation == null) {
            return false;
        }
        PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
        if (enabledProperty == null || enabledProperty.getValues().isEmpty()) {
            return false;
        }
        ValueWrapper value = enabledProperty.getValues().get(0);
        if (value.getValue() == null) {
            return false;
        }
        ActivationStatusType status = (ActivationStatusType) value.getValue().getValue();
        return ActivationStatusType.DISABLED.equals(status);
    }

//    private ActivationStatusType getActivationStatus(PrismObject object, QName property) {
//        PrismProperty prismProperty = object.findProperty(new ItemPath(ShadowType.F_ACTIVATION, property));
//        if (prismProperty == null || prismProperty.isEmpty()) {
//            return null;
//        }
//
//        return (ActivationStatusType) prismProperty.getRealValue();
//    }

    protected Component createHeader(String id, IModel<ObjectWrapper> model) {
        H3Header header = new H3Header(id, model) {

            @Override
            protected List<InlineMenuItem> createMenuItems() {
                return createDefaultMenuItems(getModel());
            }
        };

        return header;
    }

    protected List<InlineMenuItem> createDefaultMenuItems(IModel<ObjectWrapper> model) {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

        InlineMenuItem item = new InlineMenuItem(createMinMaxLabel(model), createMinMaxAction(model));
        items.add(item);

        item = new InlineMenuItem(createEmptyLabel(model), createEmptyAction(model));
        items.add(item);

        return items;
    }

    private InlineMenuItemAction createEmptyAction(final IModel<ObjectWrapper> model) {
        return new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setShowEmpty(!wrapper.isShowEmpty());
                target.add(PrismObjectPanel.this);
            }
        };
    }

    private IModel<String> createEmptyLabel(final IModel<ObjectWrapper> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                String key = wrapper.isShowEmpty() ? "PrismObjectPanel.hideEmpty" : "PrismObjectPanel.showEmpty";
                return new StringResourceModel(key, PrismObjectPanel.this, null, key).getString();
            }
        };
    }

    private InlineMenuItemAction createMinMaxAction(final IModel<ObjectWrapper> model) {
        return new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setMinimalized(!wrapper.isMinimalized());
                target.add(PrismObjectPanel.this);
            }
        };
    }

    private IModel<String> createMinMaxLabel(final IModel<ObjectWrapper> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ObjectWrapper wrapper = model.getObject();
                String key = wrapper.isMinimalized() ? "PrismObjectPanel.maximize" : "PrismObjectPanel.minimize";
                return new StringResourceModel(key, PrismObjectPanel.this, null, key).getString();
            }
        };
    }

    private void initLayout(final IModel<ObjectWrapper> model, ResourceReference image, final Form form) {
        add(createHeader(ID_HEADER, model));

        WebMarkupContainer headerPanel = new WebMarkupContainer("headerPanel");
        headerPanel.add(new AttributeAppender("class", createHeaderClassModel(model), " "));
//        TODO - attempt to fix row color application when certain actions performed, similar to AssignmentEditorPanel.
//        headerPanel.add(AttributeModifier.append("class", createHeaderClassModel(model)));
//        headerPanel.setOutputMarkupId(true);
        add(headerPanel);
        headerPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isShowHeader();
            }
        });

        Image shield = new Image("shield", new PackageResourceReference(ImgResources.class, ImgResources.SHIELD));
        shield.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ObjectWrapper wrapper = model.getObject();
                return wrapper.isProtectedAccount();
            }
        });
        headerPanel.add(shield);

        Label header = new Label("header", createDisplayName(model));
        header.add(new AttributeAppender("class", createHeaderNameClassModel(model), " "));
        header.add(createHeaderOnClickBehaviour(model));
        headerPanel.add(header);
        Label description = new Label("description", createDescription(model));
        description.add(new AttributeModifier("title", createDescription(model)));

        description.add(createHeaderOnClickBehaviour(model));
        headerPanel.add(description);

        Image headerImg = new Image("headerImg", image);
        headerImg.add(createHeaderOnClickBehaviour(model));
        headerPanel.add(headerImg);

        initButtons(headerPanel, model);

        WebMarkupContainer body = new WebMarkupContainer("body");
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ObjectWrapper wrapper = model.getObject();
                return !wrapper.isMinimalized();
            }
        });
        add(body);

        ListView<ContainerWrapper> containers = new ListView<ContainerWrapper>("containers",
                createContainerModel(model)) {

            @Override
            protected void populateItem(ListItem<ContainerWrapper> item) {
                createContainerPanel(item, form);
            }
        };
        containers.setReuseItems(true);
        body.add(containers);
    }

    protected IModel<List<ContainerWrapper>> createContainerModel(IModel<ObjectWrapper> model){
        return new PropertyModel<>(model, "containers");
    }

    protected void createContainerPanel(ListItem<ContainerWrapper> item, Form form){
        item.add(new PrismContainerPanel("container", item.getModel(), true, form, pageBase));
    }

    protected IModel<String> createDisplayName(IModel<ObjectWrapper> model) {
        return new PropertyModel<>(model, "displayName");
    }

    protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
        return new PropertyModel<>(model, "description");
    }

    private void initButtons(WebMarkupContainer headerPanel, final IModel<ObjectWrapper> model) {
        headerPanel.add(new PrismOptionButtonPanel("optionButtons", model) {

            @Override
            public void checkBoxOnUpdate(AjaxRequestTarget target) {
                target.add(PrismObjectPanel.this);
            }

            @Override
            public void minimizeOnClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setMinimalized(!wrapper.isMinimalized());
                target.add(PrismObjectPanel.this);
            }

            @Override
            public void showEmptyOnClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setShowEmpty(!wrapper.isShowEmpty());
                target.add(PrismObjectPanel.this);
            }
        });
        headerPanel.add(createOperationPanel("operationButtons"));
    }

    protected Panel createOperationPanel(String id) {
        return new EmptyPanel(id);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    public void headerOnClickPerformed(AjaxRequestTarget target, IModel<ObjectWrapper> model) {
        ObjectWrapper wrapper = model.getObject();
        wrapper.setMinimalized(!wrapper.isMinimalized());
        target.add(PrismObjectPanel.this);
    }
}
