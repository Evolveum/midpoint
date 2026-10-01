package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Locale;

public class LocaleAutoCompleteRenderer extends AbstractAutoCompleteTextRenderer<Object>{
    @Serial private static final long serialVersionUID = 1L;

    private final LookupTableType lookupTable;

    public LocaleAutoCompleteRenderer(LookupTableType lookupTable) {
        super();
        this.lookupTable = lookupTable;
    }

    @Override
    protected String getTextValue(final Object object) {
        return object.toString();
    }

    //Almost the same method as in the parent AbstractAutoCompleteRenderer except of adding "lang"
    //and "aria-label" attributes
    @Override
    public void render(final Object object, final Response response, final String criteria) {
        String textValue = getTextValue(object);
        if (textValue == null) {
            throw new IllegalStateException(
                    "A call to textValue(Object) returned an illegal value: null for object: " +
                            object.toString());
        }
        String rawTextValue = textValue;
        textValue = Strings.escapeMarkup(textValue).toString();

        response.write("<li textvalue=\"" + textValue + "\"");

        String lang = getLangValue(textValue);
        if (lang != null) {
            //the "lang" attribute must be a valid BCP47 tag (hyphen-separated); the lookup table's
            //"key" column historically used Java Locale.toString() style (underscore-separated), so
            //normalize defensively regardless of what's actually stored
            String bcp47Lang = lang.replace('_', '-');
            response.write(" lang=\"" + Strings.escapeMarkup(bcp47Lang) + "\"");

            //the accessible name must contain the visible text (SC 2.5.3 Label in Name); the current-locale
            //display name is only appended as a pronounceable fallback for scripts the active screen
            //reader voice can't read, not used as a replacement for the visible native name
            String ariaLabelValue = rawTextValue;
            String displayName = getDisplayNameInCurrentLocale(bcp47Lang);
            if (StringUtils.isNotBlank(displayName) && !displayName.equalsIgnoreCase(rawTextValue)) {
                ariaLabelValue = rawTextValue + ", " + displayName;
            }
            response.write(" aria-label=\"" + Strings.escapeMarkup(ariaLabelValue) + "\"");
        }

        final CharSequence handler = getOnSelectJavaScriptExpression(object);
        if (handler != null) {
            response.write(" onselect=\"" + Strings.escapeMarkup(handler) + '"');
        }
        response.write(">");
        renderChoice(object, response, criteria);
        response.write("</li>");
    }

    private @Nullable String getLangValue(@NotNull String localeValue) {
        var localeRow = lookupTable.getRow()
                .stream()
                .filter(row -> localeValue.equals(LocalizationUtil.translatePolyString(row.getLabel())))
                .findAny()
                .orElse(null);
        if (localeRow != null) {
            return localeRow.getKey();
        }
        return null;
    }

    private @Nullable String getDisplayNameInCurrentLocale(@NotNull String bcp47LanguageTag) {
        Locale rowLocale = Locale.forLanguageTag(bcp47LanguageTag);
        if (StringUtils.isBlank(rowLocale.getLanguage())) {
            return null;
        }
        return rowLocale.getDisplayName(LocalizationUtil.findLocale());
    }
}
