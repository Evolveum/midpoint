package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

public interface SelectableRow<S extends Serializable> extends Serializable {

    boolean isSelected();
    void setSelected(boolean selected);

}
