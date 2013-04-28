/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.resource.img;

import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public class ImgResources {

    public static final String BASE_PATH = "/img";

    public static final String USER_PRISM = "user_prism.png";
    public static final String DECISION_PRISM = "decision_prism.png";
    public static final String ROLE_PRISM = "role_prism.png";
    public static final String TRACKING_PRISM = "tracking_prism.png";
    public static final String HDD_PRISM = "hdd_prism.png";
    public static final String USER_SUIT = "user_suit.png";
    public static final String DRIVE = "drive.png";

    public static final String BUILDING = "building.png";
    public static final String MEDAL_GOLD_3 = "medal_gold_3.png";
    public static final String MEDAL_SILVER_2 = "medal_silver_2.png";
    public static final String ERROR = "error.png";

    public static final String TOOLTIP_INFO = "tooltip_info.png";

    public static final String DELETED_VALUE = "DeletedValue.png";
    public static final String PRIMARY_VALUE = "PrimaryValue.png";
    public static final String SECONDARY_VALUE = "SecondaryValue.png";

    public static final String SHIELD = "shield.png";

    public static PackageResourceReference createReference(String value) {
        return new PackageResourceReference(ImgResources.class, value);
    }
}
