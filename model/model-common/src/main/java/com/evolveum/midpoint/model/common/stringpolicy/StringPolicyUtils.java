/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

/**
 * @author mamut
 */
public class StringPolicyUtils {

    private static final String ASCII7_CHARS = " !\"#$%&'()*+,-.01234567890:;<=>?"
            + "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_" + "`abcdefghijklmnopqrstuvwxyz{|}~";

    public static StringPolicyType normalize(StringPolicyType sp) {
        if (null == sp) {
            throw new IllegalArgumentException("Providide string policy cannot be null");
        }

        if (null == sp.getLimitations()) {
            LimitationsType sl = new LimitationsType();
            sl.setCheckAgainstDictionary(false);
            sl.setCheckPattern("");
            sl.setMaxLength(Integer.MAX_VALUE);
            sl.setMinLength(0);
            sl.setMinUniqueChars(0);
            sp.setLimitations(sl);
        }

        // Add default char class
        if (null == sp.getCharacterClass()) {
            CharacterClassType cct = new CharacterClassType();
            cct.setValue(ASCII7_CHARS);
            sp.setCharacterClass(cct);
        }

        return sp;
    }

    /**
     * Prepare usable list of strings for generator
     *
     */


    public static String collectCharacterClass(CharacterClassType cc, QName ref) {
        StrBuilder l = new StrBuilder();
        if (null == cc) {
            throw new IllegalArgumentException("Character class cannot be null");
        }

        if (null != cc.getValue() && (null == ref || ref.equals(cc.getName()))) {
            l.append(cc.getValue());
        } else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
            // Process all sub lists
            for (CharacterClassType subClass : cc.getCharacterClass()) {
                // If we found requested name or no name defined
                if (null == ref || ref.equals(cc.getName())) {
                    l.append(collectCharacterClass(subClass, null));
                } else {
                    l.append(collectCharacterClass(subClass, ref));
                }
            }
        }
        // Remove duplicity in return;
        HashSet<String> h = new HashSet<>();
        for (String s : l.toString().split("")) {
            h.add(s);
        }
        return new StrBuilder().appendAll(h).toString();
    }

    /**z
     * Convert string to array
     * @param in
     * @return ArrayList
     */
    public static List<String> stringTokenizer(String in) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < in.length(); i++) {
            l.add(in.substring(i, i+1));
        }
        return l;
    }
}
