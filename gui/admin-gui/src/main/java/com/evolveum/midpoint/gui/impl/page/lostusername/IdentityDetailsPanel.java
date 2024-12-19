/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.login.module.PageFocusIdentification;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.util.AuthenticationSequenceTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IdentityDetailsPanel<F extends FocusType> extends BasePanel<F> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = IdentityDetailsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(IdentityDetailsPanel.class);
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_ARCHETYPE_LABEL = "archetypeLabel";
    private static final String ID_PHOTO = "photo";
    private static final String ID_EXPAND_BUTTON = "expandButton";
    private static final String ID_ARROW_ICON = "arrowIcon";
    private static final String ID_ITEMS_PANEL = "itemsPanel";
    private static final String ID_ITEM_NAME = "itemName";
    private static final String ID_ITEM_VALUE = "itemValue";
    private static final String ID_CONFIRM_IDENTITY = "confirmIdentity";

    private boolean expanded;
    private LoadableModel<List<ItemPathType>> itemsModel;
    private SecurityPolicyType securityPolicy;

    public IdentityDetailsPanel(String panelId, IModel<F> identityObject, SecurityPolicyType securityPolicy, boolean expanded) {
        super(panelId, identityObject);
        this.expanded = expanded;
        this.securityPolicy = securityPolicy;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        itemsModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemPathType> load() {
                return loadItemToDisplayList();
            }
        };
    }

    private List<ItemPathType> loadItemToDisplayList() {
        var identityRecoveryConfig = securityPolicy.getIdentityRecovery();
        List<ItemPathType> itemsToDisplay = identityRecoveryConfig.getItemToDisplay();
        if (itemsToDisplay == null) {
            itemsToDisplay = new ArrayList<>();
        }
        if (itemsToDisplay.size() == 0) {
            itemsToDisplay.add(new ItemPathType(FocusType.F_NAME));
        }
        return itemsToDisplay;
    }

    private void initLayout() {
        initHeaderPanel();
        initDetailsPanel();
    }

    private void initHeaderPanel() {
        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);
        add(headerPanel);

        NonCachingImage img = new NonCachingImage(ID_PHOTO, getImageResource());
        img.add(AttributeAppender.append("alt",
                LocalizationUtil.translate("IdentityDetailsPanel.image.alt", new Object[]{ getDisplayNameModel().getObject() })));
        headerPanel.add(img);

        Label displayNameLabel = new Label(ID_DISPLAY_NAME, getDisplayNameModel());
        headerPanel.add(displayNameLabel);

        Label archetypeLabel = new Label(ID_ARCHETYPE_LABEL, getArchetypeModel());
        headerPanel.add(archetypeLabel);

        AjaxButton arrowButton = new AjaxButton(ID_EXPAND_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                expanded = !expanded;
                ajaxRequestTarget.add(IdentityDetailsPanel.this);
            }
        };

        arrowButton.add(AttributeAppender.append("aria-label",
                () -> expanded ?
                        LocalizationUtil.translate("IdentityDetailsPanel.expandButton.collapse", new Object[]{ getDisplayNameModel().getObject() }) :
                        LocalizationUtil.translate("IdentityDetailsPanel.expandButton.expand", new Object[]{ getDisplayNameModel().getObject() })));

        arrowButton.setOutputMarkupId(true);
        headerPanel.add(arrowButton);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ARROW_ICON);
        icon.add(AttributeAppender.append("class", this::getArrowIconCss));
        arrowButton.add(icon);

        AjaxButton confirmIdentity = new AjaxButton(ID_CONFIRM_IDENTITY) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                identityConfirmed(getFocusObject(), ajaxRequestTarget);
            }
        };
        add(confirmIdentity);
    }

    private void initDetailsPanel() {
        ListView<ItemPathType> itemsPanel = new ListView<>(ID_ITEMS_PANEL, itemsModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> attributeItem) {
                Label itemName = new Label(ID_ITEM_NAME, resolveItemName(attributeItem.getModelObject()));
                itemName.setOutputMarkupId(true);
                attributeItem.add(itemName);

                Label itemValue = new Label(ID_ITEM_VALUE, resolveItemValue(attributeItem.getModelObject()));
                itemValue.setOutputMarkupId(true);
                attributeItem.add(itemValue);
            }
        };
        itemsPanel.setOutputMarkupId(true);
        itemsPanel.add(new VisibleBehaviour(() -> expanded));
        add(itemsPanel);
    }

    private AbstractResource getImageResource() {
        byte[] photo = null;
        if (getModelObject() instanceof UserType user) {
            photo = user.getJpegPhoto();
            if (photo == null) {
                URL defaultImage = this.getClass().getClassLoader().getResource("static/img/placeholder.png");
                if (defaultImage == null) {
                    return null;
                }
                try {
                    photo = IOUtils.toByteArray(defaultImage);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return new ByteArrayResource("image/jpeg", photo);
    }

    private IModel<String> getDisplayNameModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return WebComponentUtil.getDisplayNameOrName(getModelObject().asPrismObject());
            }
        };
    }

    private IModel<String> getArchetypeModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                DisplayType archetypeDisplay =
                        GuiDisplayTypeUtil.getArchetypePolicyDisplayType(getModelObject().asPrismObject(), getParentPage());
                return GuiDisplayTypeUtil.getTranslatedLabel(archetypeDisplay);
            }
        };
    }

    private String getArrowIconCss() {
        return expanded ? GuiStyleConstants.CLASS_ICON_COLLAPSE : GuiStyleConstants.CLASS_ICON_EXPAND;
    }

    private IModel<String> resolveItemName(ItemPathType itemPath) {
        return () -> {
            ItemDefinition<?> def = getModelObject().asPrismObject().getDefinition().findItemDefinition(itemPath.getItemPath());
            return WebComponentUtil.getItemDefinitionDisplayNameOrName(def);
        };
    }

    private IModel<String> resolveItemValue(ItemPathType itemPath) {
        return () -> {
            var item = getModelObject().asPrismObject().findItem(itemPath.getItemPath());
            var value = item != null ? item.getRealValue() : null;
            return value == null ? "" : value.toString();
        };
    }

    private void identityConfirmed(FocusType focus, AjaxRequestTarget target) {
        Class<? extends PageAdminLTE> page = getParentPage().runPrivileged((Producer<Class<? extends PageAdminLTE>>) () -> {
            Task task = getParentPage().createAnonymousTask(OPERATION_GET_SECURITY_POLICY);

            try {
                AbstractAuthenticationModuleType module = AuthUtil.getFirstModuleOfDefaultChannel(
                        getParentPage().getModelInteractionService().getSecurityPolicy(focus.asPrismObject(), task, task.getResult()));

                if (module instanceof LoginFormAuthenticationModuleType) {
                    return PageLogin.class;
                } else if (module instanceof FocusIdentificationAuthenticationModuleType) {
                    return PageFocusIdentification.class;
                }

            } catch (CommonException e) {
                LOGGER.debug("Couldn't load security policy for " + focus, e);
            }
            return getParentPage().getMidpointApplication().getHomePage();
        });

        AuthUtil.clearMidpointAuthentication();
        PageParameters parameters = new PageParameters();
        parameters.add("name", focus.getName());
        setResponsePage(page, parameters);
    }

    private F getFocusObject() {
        return getModelObject();
    }

}
