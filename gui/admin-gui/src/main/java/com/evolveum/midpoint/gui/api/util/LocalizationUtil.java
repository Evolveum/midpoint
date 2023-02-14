package com.evolveum.midpoint.gui.api.util;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
        Locale locale = findLocale();
        return translate(key, params, defaultMsg, locale);
    }

    public static String translate(String key, Object[] params, String defaultMsg, Locale locale) {
        MidPointApplication ma = MidPointApplication.get();
        LocalizationService service = ma.getLocalizationService();

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

    public static String translatePolyString(PolyStringType poly) {
        Locale locale = findLocale();

        return translatePolyString(PolyString.toPolyString(poly), locale);
    }

    public static String translatePolyString(PolyStringType poly, Locale locale) {
        return translatePolyString(PolyString.toPolyString(poly), locale);
    }

    public static String translatePolyString(PolyString poly) {
        Locale locale = findLocale();

        return translatePolyString(poly, locale);
    }

    public static String translatePolyString(PolyString poly, Locale locale) {
        if (poly == null) {
            return null;
        }

        LocalizationService localizationService = MidPointApplication.get().getLocalizationService();

        String translatedValue = localizationService.translate(poly, locale, true);
        String translationKey = poly.getTranslation() != null ? poly.getTranslation().getKey() : null;

        if (StringUtils.isNotEmpty(translatedValue) && !translatedValue.equals(translationKey)) {
            return translatedValue;
        }

        return poly.getOrig();
    }
}
