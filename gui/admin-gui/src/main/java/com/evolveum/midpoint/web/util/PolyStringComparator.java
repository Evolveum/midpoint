/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.text.Collator;
import java.util.Comparator;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PolyStringComparator implements Comparator<PolyString> {

    public static final PolyStringComparator COMPARE_TRANSLATED = new PolyStringComparator(true);

    public static final PolyStringComparator COMPARE_ORIG = new PolyStringComparator(false);

    private boolean translate;

    public PolyStringComparator(boolean translate) {
        this.translate = translate;
    }

    @Override
    public int compare(PolyString o1, PolyString o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        Collator collator = WebComponentUtil.getCollator();
        String s1 = translate ? WebComponentUtil.getTranslatedPolyString(o1) : o1.getOrig();
        String s2 = translate ? WebComponentUtil.getTranslatedPolyString(o2) : o2.getOrig();

        return collator.compare(s1, s2);
    }
}
