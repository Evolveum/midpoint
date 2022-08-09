/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogTile<T extends Serializable> extends Tile<T> {

    private String description;

    private String info;

    public CatalogTile() {
        this(null, null);
    }

    public CatalogTile(String icon, String title) {
        super(icon, title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
