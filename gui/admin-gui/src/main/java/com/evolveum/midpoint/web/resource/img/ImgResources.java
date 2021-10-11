/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.resource.img;

import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public class ImgResources {

    public static final String BASE_PATH = "/img";

    public static final String USER_PRISM = "user_prism.png";
    public static final String HDD_PRISM = "hdd_prism.png";
    public static final String ERROR = "error.png";
    public static final String SHIELD = "shield.png";

    public static PackageResourceReference createReference(String value) {
        return new PackageResourceReference(ImgResources.class, value);
    }
}
