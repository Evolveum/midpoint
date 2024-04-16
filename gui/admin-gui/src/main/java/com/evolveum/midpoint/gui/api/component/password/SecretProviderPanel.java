/*
 * Copyright (C) 2018-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.SecretsResolver;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Panel for SecretProvider
 *
 * @author skublik
 */
public class SecretProviderPanel extends BasePanel<ExternalDataType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_IDENTIFIERS = "identifiers";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_PROVIDER_IDENTIFIER_DROPDOWN_BUTTON = "providerIdentifierDropdownButton";
    private static final String ID_PROVIDER_IDENTIFIER_LABEL = "providerIdentifierLabel";
    private static final String ID_PROVIDER_KEY = "providerKey";

    private LoadableDetachableModel<List<SecretProvider>> secretProviders;

    public SecretProviderPanel(String id, IModel<ExternalDataType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        secretProviders = new LoadableDetachableModel<>() {
            @Override
            protected List<SecretProvider> load() {
                ArrayList<SecretProvider> identifiers = new ArrayList<>();

                Protector protector = getProtector();
                if (protector instanceof SecretsResolver resolver) {
                    identifiers.addAll(
                            getParentPage().getSecretsProviderManager().getSecretsProviderDescriptions(resolver)
                                    .entrySet().stream()
                                    .map(entry -> new SecretProvider(
                                            entry.getKey(),
                                            LocalizationUtil.translatePolyString(entry.getValue().getLabel())))
                                    .toList());
                }
                return identifiers;
            }
        };
    }

    private void initLayout() {

        AjaxButton dropdownButton = new AjaxButton(ID_PROVIDER_IDENTIFIER_DROPDOWN_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        };
        dropdownButton.setOutputMarkupId(true);
        add(dropdownButton);

        IModel<String> providerIdentifierModel = new IModel<>() {
            @Override
            public String getObject() {
                if (getModelObject() == null || StringUtils.isEmpty(getModelObject().getProvider())) {
                    return LocalizationUtil.translate("SecretProviderPanel.emptyIdentifier");
                }
                Optional<SecretProvider> secretProvider = getIdentifiers().stream()
                        .filter(provider -> provider.identifier.equals(getModelObject().getProvider()))
                        .findFirst();
                return secretProvider.isPresent() ? secretProvider.get().displayName : getModelObject().getProvider();
            }

            @Override
            public void setObject(String object) {
                ExternalDataType current = getModelObject();
                if (current == null) {
                    current = new ExternalDataType();
                }

                current.setProvider(object);
                getModel().setObject(current);
            }
        };

        Label dropdownButtonLabel = new Label(ID_PROVIDER_IDENTIFIER_LABEL, providerIdentifierModel);
        dropdownButtonLabel.setOutputMarkupId(true);
        dropdownButton.add(dropdownButtonLabel);

        ListView<SecretProvider> menuItems = new ListView<>(ID_IDENTIFIERS, Model.ofList(getIdentifiers())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SecretProvider> item) {
                AjaxButton ajaxLinkPanel = new AjaxButton(ID_IDENTIFIER, () -> item.getModelObject().displayName) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        providerIdentifierModel.setObject(item.getModelObject().identifier);

                        target.add(SecretProviderPanel.this);
                        target.add(SecretProviderPanel.this.get(
                                createComponentPath(ID_PROVIDER_IDENTIFIER_DROPDOWN_BUTTON, ID_PROVIDER_IDENTIFIER_LABEL)));

                        if (getKeyTextPanel().getRawInput() != null) {
                            getKeyTextPanel().validate();
                            refreshFeedback(target);
                        }
                    }
                };

                item.add(ajaxLinkPanel);
            }
        };
        menuItems.setOutputMarkupId(true);
        add(menuItems);

        IModel<String> providerKeyModel = new IModel<>() {
            @Override
            public String getObject() {
                if (getModelObject() == null || StringUtils.isEmpty(getModelObject().getKey())) {
                    return null;
                }
                return getModelObject().getKey();
            }

            @Override
            public void setObject(String object) {
                ExternalDataType current = getModelObject();
                if (current == null) {
                    current = new ExternalDataType();
                }

                current.setKey(object);
                getModel().setObject(current);
            }
        };

        TextPanel<String> providerKeyPanel = new TextPanel<>(ID_PROVIDER_KEY, providerKeyModel, String.class, false);
        providerKeyPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshFeedback(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                refreshFeedback(target);
            }
        });
        providerKeyPanel.getBaseFormComponent().add(new AjaxEventBehavior("submit") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                refreshFeedback(target);
            }
        });
        providerKeyPanel.getBaseFormComponent().add(AttributeAppender.append(
                "placeholder",
                LocalizationUtil.translate("SecretProviderPanel.key.placeholder")));
        providerKeyPanel.setOutputMarkupId(true);
        add(providerKeyPanel);
    }

    protected void refreshFeedback(AjaxRequestTarget target) {
    }

    private List<SecretProvider> getIdentifiers() {
        return secretProviders.getObject();
    }

    private Protector getProtector() {
        return ((MidPointApplication) Application.get()).getProtector();
    }

    class SecretProvider implements Serializable {

        private final String identifier;
        private final String displayName;

        private SecretProvider(String identifier, String displayName) {
            this.identifier = identifier;
            this.displayName = displayName;
        }
    }

    public FormComponent getKeyTextPanel() {
        return ((TextPanel) get(ID_PROVIDER_KEY)).getBaseFormComponent();
    }

}
