/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogTile<T extends Serializable> extends Tile<T> {

    private String logo;

    private String description;

    public CatalogTile() {
        this(null, null);
    }

    public CatalogTile(String icon, String title) {
        super(icon, title);
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
