package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

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

    //Almost the same method as in the parent AbstractAutoCompleteRenderer except of adding "lang" attribute
    @Override
    public void render(final Object object, final Response response, final String criteria) {
        String textValue = getTextValue(object);
        if (textValue == null) {
            throw new IllegalStateException(
                    "A call to textValue(Object) returned an illegal value: null for object: " +
                            object.toString());
        }
        textValue = Strings.escapeMarkup(textValue).toString();

        response.write("<li textvalue=\"" + textValue + "\"");

        //add lang attribute in order to satisfy accessibility requirement
        String lang = getLangValue(textValue);
        if (lang != null) {
            response.write(" lang=\"" + Strings.escapeMarkup(lang) + "\"");
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
}
