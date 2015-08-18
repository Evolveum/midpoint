package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItem<T extends Serializable> implements Serializable {

    public static final String F_VALUE = "value";
    public static final String F_DISPLAY_VALUE = "displayValue";

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER
    }

    private Search search;

    private ItemDefinition definition;
    private T value;
    private String displayValue;

    public SearchItem(Search search, ItemDefinition definition) {
        Validate.notNull(definition, "Item definition must not be null.");

        if (!(definition instanceof PrismPropertyDefinition)
                && !(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition type '" + definition + "'");
        }

        this.search = search;
        this.definition = definition;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return definition.getName().getLocalPart();
    }

    public Type getType() {
        if (definition instanceof PrismReferenceDefinition) {
            return Type.BROWSER;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) definition;
        if (def.getAllowedValues() != null && !def.getAllowedValues().isEmpty()) {
            return Type.ENUM;
        }

        if (DOMUtil.XSD_BOOLEAN.equals(def.getTypeName())) {
            return Type.BOOLEAN;
        }

        return Type.TEXT;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public Search getSearch() {
        return search;
    }
}
