/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
