/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

/**
 * @param <S> unused here, but needed to smuggle type information; TODO: suspicious usage though, can't we do it better?
 */
@SuppressWarnings("unused")
public interface SelectableRow<S extends Serializable> extends Serializable {

    boolean isSelected();
    void setSelected(boolean selected);
}
