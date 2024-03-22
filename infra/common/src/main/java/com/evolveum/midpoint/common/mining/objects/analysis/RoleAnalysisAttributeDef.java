package com.evolveum.midpoint.common.mining.objects.analysis;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;

public class RoleAnalysisAttributeDef implements Serializable {

    ItemPath path;
    boolean isContainer;
    String displayValue;
    public RoleAnalysisAttributeDef(ItemPath path, boolean isContainer) {
        this.path = path;
        this.isContainer = isContainer;
    }

    public RoleAnalysisAttributeDef(ItemPath path, boolean isContainer, String displayValue) {
        this.path = path;
        this.isContainer = isContainer;
        this.displayValue = displayValue;
    }

    public ItemPath getPath() {
        return path;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String resolveSingleValueItem(PrismObject<?> prismObject, ItemPath itemPath) {
        if (!isContainer) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
            if (property != null) {
                Object object = property.getRealValue();
                return extractRealValue(object);
            }
        }
        return null;
    }

    public Set<String> resolveMultiValueItem(PrismObject<?> prismObject, ItemPath itemPath) {
        return null;
    }


    protected static String extractRealValue(Object object) {
        if (object != null) {
            if (object instanceof PolyString) {
                return ((PolyString) object).getOrig();
            } else if (object instanceof PrismPropertyValueImpl) {
                Object realValue = ((PrismPropertyValueImpl<?>) object).getRealValue();
                if (realValue != null) {
                    return realValue.toString();
                }
            } else {
                return object.toString();
            }
        }
        return null;
    }
}
