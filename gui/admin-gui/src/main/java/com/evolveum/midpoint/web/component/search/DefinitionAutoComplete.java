package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class DefinitionAutoComplete extends AutoCompleteTextField<ItemDefinition> {

    private static final Trace LOGGER = TraceManager.getTrace(DefinitionAutoComplete.class);

    private IModel<List<ItemDefinition>> allChoices;

    public DefinitionAutoComplete(String id, IModel<ItemDefinition> model,
                                  IModel<List<ItemDefinition>> allChoices) {
        super(id, model, ItemDefinition.class, new DefinitionRendererConverter(allChoices), new AutoCompleteSettings());

        this.allChoices = allChoices;
    }

    @Override
    protected Iterator<ItemDefinition> getChoices(String input) {
        LOGGER.trace("Searching items for autocomplete for input {}", input);
        List<ItemDefinition> definitions = allChoices.getObject();

        if (StringUtils.isNotEmpty(input)) {
            input = input.trim().toLowerCase();
        }

        List<ItemDefinition> values = new ArrayList<>();
        for (ItemDefinition def : definitions) {
            String name = DefinitionRendererConverter.getItemDefinitionName(def);

            if (name.toLowerCase().contains(input)) {
                LOGGER.trace("Item matched {} > {}, {}", input, name, def);

                values.add(def);
            }
        }

        Collections.sort(values, new Comparator<ItemDefinition>() {

            @Override
            public int compare(ItemDefinition o1, ItemDefinition o2) {
                String n1 = DefinitionRendererConverter.getItemDefinitionName(o1);
                String n2 = DefinitionRendererConverter.getItemDefinitionName(o2);

                return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
            }
        });

        return values.iterator();
    }

    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) new DefinitionRendererConverter(allChoices);
    }

    private static class DefinitionRendererConverter extends AbstractAutoCompleteTextRenderer<ItemDefinition>
            implements IConverter<ItemDefinition> {

        private IModel<List<ItemDefinition>> allChoices;

        public DefinitionRendererConverter(IModel<List<ItemDefinition>> allChoices) {
            this.allChoices = allChoices;
        }

        @Override
        public ItemDefinition convertToObject(String value, Locale locale) throws ConversionException {
            if (value == null) {
                return null;
            }

            List<ItemDefinition> all = allChoices.getObject();
            for (ItemDefinition def : all) {
                String name = getItemDefinitionName(def);
                if (name.equals(value)) {
                    return def;
                }
            }

            return null;
        }

        @Override
        public String convertToString(ItemDefinition value, Locale locale) {
            return getItemDefinitionName(value);
        }

        @Override
        protected String getTextValue(ItemDefinition object) {
            return getItemDefinitionName(object);
        }

        static String getItemDefinitionName(ItemDefinition def) {
            if (def == null) {
                return null;
            }

            String name = def.getDisplayName();
            if (StringUtils.isEmpty(name)) {
                name = def.getName().getLocalPart();
            }
            return name;
        }
    }
}
