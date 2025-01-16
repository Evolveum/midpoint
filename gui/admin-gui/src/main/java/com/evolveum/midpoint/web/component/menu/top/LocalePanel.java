/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import java.util.Locale;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.common.AvailableLocale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class LocalePanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);

    private static final String FLAG_CLASS_PREFIX = "fi fi-";

    public LocalePanel(String id) {
        super(id);
    }

    protected IModel<String> getSelectedFlagIcon() {
        return () -> getFlagIcon(getSelectedLocaleDescriptor());
    }

    public static String getFlagIcon(AvailableLocale.LocaleDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        return FLAG_CLASS_PREFIX + descriptor.getFlag();
    }

    protected AvailableLocale.LocaleDescriptor getSelectedLocaleDescriptor() {
        Locale locale = getSession().getLocale();
        if (locale == null) {
            return null;
        }

        // The second condition is a fix attempt for issue MID-2075, where firefox
        // returns 'sk' as a locale from session, while other browsers return 'sk_SK'.
        // This is the reason, why in firefox selected locale is ignored (the commented
        // condition is not met) so we are adding second condition to overcome this issue.
        for (AvailableLocale.LocaleDescriptor desc : AvailableLocale.AVAILABLE_LOCALES) {
//            if (locale.equals(desc.getLocale())
            if (locale.equals(desc.getLocale()) || locale.getLanguage().equals(desc.getLocale().getLanguage())) {
                return desc;
            }
        }

        return null;
    }

    protected void changeLocale(AjaxRequestTarget target, AvailableLocale.LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", descriptor.getLocale());
        getSession().setLocale(descriptor.getLocale());
        if (AuthUtil.getPrincipalUser() != null) {
            AuthUtil.getPrincipalUser().setPreferredLocale(descriptor.getLocale());
        }
        WebComponentUtil.getCompiledGuiProfile().setLocale(descriptor.getLocale());

        getParentPage().changeLocal(target);

        target.add(getPage());
    }
}
