package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItem<T extends Serializable> implements Serializable {

    public static final String F_VALUES = "values";

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER
    }

    private Search search;

    private ItemPath path;
    private ItemDefinition definition;
    private List<DisplayableValue<T>> values;

    public SearchItem(Search search, ItemPath path, ItemDefinition definition) {
        Validate.notNull(path, "Item path must not be null.");
        Validate.notNull(definition, "Item definition must not be null.");

        if (!(definition instanceof PrismPropertyDefinition)
                && !(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition type '" + definition + "'");
        }

        this.search = search;
        this.path = path;
        this.definition = definition;
    }

    public ItemPath getPath() {
        return path;
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

    public List<DisplayableValue<T>> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }

    public void setValues(List<DisplayableValue<T>> values) {
        this.values = values;
    }

    public Search getSearch() {
        return search;
    }

    public List<DisplayableValue> getAllowedValues() {
        List<DisplayableValue> list = new ArrayList();
        if (!(definition instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) definition;
        list.addAll(def.getAllowedValues());

        return list;
    }
}
