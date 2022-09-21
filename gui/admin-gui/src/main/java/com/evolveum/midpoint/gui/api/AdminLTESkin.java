/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AdminLTESkin {
    private static final Map<String, String> ORIGINAL_SKINS;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("skin-blue", "blue");
        map.put("skin-blue-light", "lightblue");
        map.put("skin-yellow", "yellow");
        map.put("skin-yellow-light", "yellow");
        map.put("skin-green", "green");
        map.put("skin-green-light", "green");
        map.put("skin-purple", "purple");
        map.put("skin-purple-light", "purple");
        map.put("skin-red", "red");
        map.put("skin-red-light", "red");
        map.put("skin-black", "black");
        map.put("skin-black-light", "black");

        ORIGINAL_SKINS = Collections.unmodifiableMap(map);
    }

    public static final AdminLTESkin SKIN_DEFAULT = new AdminLTESkin("lightblue", "skin-blue-light");

    private String cssColorClass;

    private String originalSkinName;

    private AdminLTESkin(String cssColorClass, String originalSkinName) {
        this.cssColorClass = cssColorClass;
        this.originalSkinName = originalSkinName;
    }

    public String getOriginalSkinName() {
        return "skin-" + originalSkinName;
    }

    public String getNavbarCss() {
        return "navbar-" + cssColorClass;
    }

    public String getSidebarCss(boolean dark) {
        return dark ? getSidebarDarkCss() : getSidebarLightCss();
    }

    public String getSidebarLightCss() {
        if ("black".equals(cssColorClass)) {
            return getSidebarDarkCss();
        }

        return "sidebar-light-" + cssColorClass;
    }

    public String getSidebarDarkCss() {
        return "sidebar-dark-" + cssColorClass;
    }

    public String getAccentCss() {
        return "accent-" + cssColorClass;
    }

    public String getBackgroundCss() {
        return "bg-" + cssColorClass;
    }

    public static @NotNull AdminLTESkin create(String originalSkin) {
        if (StringUtils.isEmpty(originalSkin)) {
            return SKIN_DEFAULT;
        }

        String css = ORIGINAL_SKINS.get(originalSkin);
        if (StringUtils.isEmpty(css)) {
            css = originalSkin;
        }

        return new AdminLTESkin(css, originalSkin);
    }
}
