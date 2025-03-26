package com.evolveum.midpoint.gui.api.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.prism.impl.binding.AbstractMutableContainerable;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ThreadContext;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.AvailableLocale;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class LocalizationUtil {

    public static @NotNull Locale findLocale() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null && principal.getLocale() != null) {
            return principal.getLocale();
        }

        if (ThreadContext.getSession() != null) {
            MidPointAuthWebSession session = MidPointAuthWebSession.get();
            if (session != null && session.getLocale() != null) {
                return session.getLocale();
            }
        }

        MidPointApplication app = MidPointApplication.get();
        LocalizationService localizationService = app.getLocalizationService();
        if (localizationService.getDefaultLocale() != null) {
            return localizationService.getDefaultLocale();
        }

        return AvailableLocale.getDefaultLocale();
    }

    public static String translate(String key) {
        return translate(key, new Object[0]);
    }

    public static String translate(String key, Object param) {
        return translate(key, new Object[]{param});
    }

    public static String translate(String key, Object[] params) {
        return translate(key, params, key);
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

    public static String translateMessage(LocalizableMessageType msg) {
        if (msg == null) {
            return null;
        }

        return translateMessage(com.evolveum.midpoint.schema.util.LocalizationUtil.toLocalizableMessage(msg));
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

    public static String translateLookupTableRowLabel(@NotNull LookupTableRowType row) {
        if (row.getLabel() != null) {
            return translatePolyString(row.getLabel());
        }
        return row.getKey();
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

        // todo this should not be here, we translated what we could, there's key, fallback and fallbackMessage to handle this properly
        if (StringUtils.isNotEmpty(translatedValue) && !translatedValue.equals(translationKey)) {
            return translatedValue;
        }

        // todo this should not be here either
        if (poly.getOrig() == null) {
            return translatedValue;
        }

        return poly.getOrig();
    }

    public static String translateEnum(Enum<?> e) {
        return translateEnum(e, null);
    }

    public static String translateEnum(Enum<?> e, String nullKey) {
        if (e == null) {
            return nullKey != null ? translate(nullKey) : null;
        }

        String key = WebComponentUtil.createEnumResourceKey(e);
        return translate(key);
    }

    public static <T extends Enum> String createKeyForEnum(T value) {
        if (value == null) {
            return null;
        }

        return value.getClass().getSimpleName() + "." + value.name();
    }

    public static String translateLifecycleState(String value, PageAdminLTE parentPage) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }

        LookupTableType lookupTable = WebComponentUtil.loadLookupTable(SystemObjectsType.LOOKUP_LIFECYCLE_STATES.value(), parentPage);

        if (lookupTable == null) {
            return value;
        }

        for (LookupTableRowType row : lookupTable.getRow()) {
            if (value.equals(row.getKey())) {
                return translateLookupTableRowLabel(row);
            }
        }
        return value;
    }
}
