/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.SubscriptionType;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;

public class FooterPanel extends BasePanel<Void> {

    private static final String ID_SUBSCRIPTION_MESSAGE = "subscriptionMessage";
    private static final String ID_COPYRIGHT_MESSAGE = "copyrightMessage";
    private static final String ID_VERSION = "version";

    public FooterPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();

        add(new VisibleBehaviour(() -> !getPageBase().isErrorPage() && isFooterVisible()));
    }

    private void initLayout() {
        WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {

            private static final long serialVersionUID = 1L;

            @Deprecated
            public String getDescribe() {
                return FooterPanel.this.getDescribe();
            }
        };
        version.add(new VisibleBehaviour(() ->
                isFooterVisible() && RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())));
        add(version);

        WebMarkupContainer copyrightMessage = new WebMarkupContainer(ID_COPYRIGHT_MESSAGE);
        copyrightMessage.add(getFooterVisibleBehaviour());
        add(copyrightMessage);

        Label subscriptionMessage = new Label(ID_SUBSCRIPTION_MESSAGE,
                new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        String subscriptionId = getSubscriptionId();
                        if (StringUtils.isEmpty(subscriptionId)) {
                            return "";
                        }
                        if (!WebComponentUtil.isSubscriptionIdCorrect(subscriptionId)) {
                            return " " + createStringResource("PageBase.nonActiveSubscriptionMessage").getString();
                        }
                        if (SubscriptionType.DEMO_SUBSRIPTION.getSubscriptionType().equals(subscriptionId.substring(0, 2))) {
                            return " " + createStringResource("PageBase.demoSubscriptionMessage").getString();
                        }
                        return "";
                    }
                });
        subscriptionMessage.setOutputMarkupId(true);
        subscriptionMessage.add(getFooterVisibleBehaviour());
        add(subscriptionMessage);
    }

    /**
     * It's here only because of some IDEs - it's not properly filtering
     * resources during maven build. "describe" variable is not replaced.
     *
     * @return "unknown" instead of "git describe" for current build.
     */
    @Deprecated
    public String getDescribe() {
        return getString("pageBase.unknownBuildNumber");
    }

    private VisibleEnableBehaviour getFooterVisibleBehaviour() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isFooterVisible();
            }
        };
    }

    private boolean isFooterVisible() {
        String subscriptionId = getSubscriptionId();
        if (StringUtils.isEmpty(subscriptionId)) {
            return true;
        }
        return !WebComponentUtil.isSubscriptionIdCorrect(subscriptionId) ||
                (SubscriptionType.DEMO_SUBSRIPTION.getSubscriptionType().equals(subscriptionId.substring(0, 2))
                        && WebComponentUtil.isSubscriptionIdCorrect(subscriptionId));
    }

    private String getSubscriptionId() {
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        return info != null ? info.getSubscriptionIdentifier() : null;
    }
}
