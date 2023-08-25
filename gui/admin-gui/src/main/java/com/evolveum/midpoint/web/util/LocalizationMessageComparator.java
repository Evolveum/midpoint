/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.text.Collator;
import java.util.Comparator;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class LocalizationMessageComparator<T> implements Comparator<T> {

    public static final Comparator<LocalizableMessage> COMPARE_MESSAGE_TRANSLATED = new MessageComparator(true);

    public static final Comparator<LocalizableMessage> COMPARE_MESSAGE_UNTRANSLATED = new MessageComparator(false);

    public static final Comparator<PolyString> COMPARE_POLYSTRING_TRANSLATED = new PolyStringComparator(true);

    public static final Comparator<PolyString> COMPARE_POLYSTRING_UNTRANSLATED = new PolyStringComparator(false);

    private boolean translate;

    public LocalizationMessageComparator(boolean translate) {
        this.translate = translate;
    }

    @Override
    public int compare(T o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        Collator collator = WebComponentUtil.getCollator();
        String s1 = translate ? getTranslation(o1) : getFallback(o1);
        String s2 = translate ? getTranslation(o2) : getFallback(o2);

        return collator.compare(s1, s2);
    }

    protected abstract String getFallback(T object);

    protected abstract String getTranslation(T object);

    private static class MessageComparator extends LocalizationMessageComparator<LocalizableMessage> {

        public MessageComparator(boolean translate) {
            super(translate);
        }

        @Override
        protected String getFallback(LocalizableMessage object) {
            if (object == null) {
                return null;
            }
            return object.getFallbackMessage();

        }

        @Override
        protected String getTranslation(LocalizableMessage object) {
            return WebComponentUtil.translateMessage(object);
        }
    }

    private static class PolyStringComparator extends LocalizationMessageComparator<PolyString> {

        public PolyStringComparator(boolean translate) {
            super(translate);
        }

        @Override
        protected String getFallback(PolyString object) {
            if (object == null) {
                return null;
            }
            return object.getOrig();
        }

        @Override
        protected String getTranslation(PolyString object) {
            return LocalizationUtil.translatePolyString(object);
        }
    }
}
