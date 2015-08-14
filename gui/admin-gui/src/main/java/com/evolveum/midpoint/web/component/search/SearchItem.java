package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItem<T extends Serializable> implements Serializable {

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER
    }

    private Search search;

    private Type type;
    private ItemDefinition definition;
    private T value;
    private String displayValue;

    public SearchItem(Search search, ItemDefinition definition) {
        Validate.notNull(definition, "Item definition must not be null.");

        this.search = search;
        this.definition = definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return definition.getName().getLocalPart();
    }

    public Type getType() {
        return type;
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
