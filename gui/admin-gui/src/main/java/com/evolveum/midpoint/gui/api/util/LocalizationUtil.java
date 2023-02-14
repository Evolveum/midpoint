package com.evolveum.midpoint.gui.api.util;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

public class LocalizationUtil {

    public static @NotNull Locale findLocale() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null && principal.getLocale() != null) {
            return principal.getLocale();
        }

        MidPointAuthWebSession session = MidPointAuthWebSession.get();
        if (session != null && session.getLocale() != null) {
            return session.getLocale();
        }

        MidPointApplication app = MidPointApplication.get();
        if (app.getLocalizationService().getDefaultLocale() != null) {
            return app.getLocalizationService().getDefaultLocale();
        }

        return Locale.getDefault();
    }

    public static String translate(String key) {
        return translate(key, new Object[0]);
    }

    public static String translate(String key, Object[] params) {
        return translate(key, params, null);
    }

    public static String translate(String key, Object[] params, String defaultMsg) {
        MidPointApplication ma = MidPointApplication.get();
        LocalizationService service = ma.getLocalizationService();

        Locale locale = findLocale();
        return service.translate(key, params, locale, defaultMsg);
    }

    public static String translateMessage(LocalizableMessage msg) {
        if (msg == null) {
            return null;
        }

        MidPointApplication application = MidPointApplication.get();
        if (application == null) {
            return msg.getFallbackMessage();
        }

        return application.getLocalizationService().translate(msg, findLocale());
    }

    public static String translateLookupTableRowLabel(String lookupTableOid, LookupTableRowType row) {
        LocalizationService localizationService = MidPointApplication.get().getLocalizationService();

        String fallback = row.getLabel() != null ? row.getLabel().getOrig() : row.getKey();
        return localizationService.translate(lookupTableOid + "." + row.getKey(), new String[0], findLocale(), fallback);
    }
}
