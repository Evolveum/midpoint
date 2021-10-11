/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.polystring;

import com.evolveum.midpoint.prism.Matchable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.Recomputable;
import com.evolveum.midpoint.prism.Structured;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * Polymorphic string. String that may have more than one representation at
 * the same time. The primary representation is the original version that is
 * composed of the full Unicode character set. The other versions may be
 * normalized to trim it, normalize character case, normalize spaces,
 * remove national characters or even transliterate the string.
 *
 * PolyString is (almost) immutable. The original value is immutable, but the
 * other normalized values can be changed. However the only way to change them
 * is to recompute them from the original value.
 *
 * @author Radovan Semancik
 */
public class PolyString implements Matchable<PolyString>, Recomputable, Structured, DebugDumpable, ShortDumpable, Serializable, Comparable<Object> {
    private static final long serialVersionUID = -5070443143609226661L;

    public static final ItemName F_ORIG = new ItemName(PrismConstants.NS_TYPES, "orig");
    public static final ItemName F_NORM = new ItemName(PrismConstants.NS_TYPES, "norm");
    public static final ItemName F_TRANSLATION = new ItemName(PrismConstants.NS_TYPES, "translation");
    public static final String F_TRANSLATION_LOCAL_PART = F_TRANSLATION.getLocalPart();
    public static final ItemName F_LANG = new ItemName(PrismConstants.NS_TYPES, "lang");
    public static final String F_LANG_LOCAL_PART = F_LANG.getLocalPart();

    private String orig;
    private String norm = null;
    private PolyStringTranslationType translation;
    private Map<String,String> lang;

    public PolyString(String orig) {
        this(orig, null);
    }

    public PolyString(String orig, String norm) {
        this(orig, norm, null);
    }

    // TODO: we may need a builder for this ... hopefully I do not expect that there will be
    //  any more properties in a near future

    public PolyString(String orig, String norm, PolyStringTranslationType translation) {
        this(orig, norm, translation, null);
    }

    public PolyString(String orig, String norm, PolyStringTranslationType translation, Map<String,String> lang) {
        super();
        this.orig = orig;
        this.norm = norm;
        this.translation = translation;
        this.lang = lang;
        if (isNull()) {
            throw new IllegalArgumentException("Cannot create PolyString with all null attribute values");
        }
    }

    public String getOrig() {
        return orig;
    }

    /**
     * Used to set computed values of orig, e.g. in cases of translation. Not very clean.
     */
    public void setComputedOrig(String computedOrig) {
        this.orig = computedOrig;
    }

    public String getNorm() {
        return norm;
    }

    public PolyStringTranslationType getTranslation() {
        return translation;
    }

    public Map<String, String> getLang() {
        return lang;
    }

    /**
     * Do NOT rely on this method too much. It may disappear later, e.g. when we align PolyString and PolyString type and
     * make PolyString really immutable.
     */
    @Experimental
    public void setTranslation(PolyStringTranslationType translation) {
        this.translation = translation;
    }

    /**
     * Do NOT rely on this method too much. It may disappear later, e.g. when we align PolyString and PolyString type and
     * make PolyString really immutable.
     */
    @Experimental
    public void setLang(Map<String, String> lang) {
        this.lang = lang;
    }

    public boolean isEmpty() {
        return isOrigEmpty() && isLocalizationKeyEmpty() && isLanguageMapEmpty();
    }

    public boolean isNull() {
        return orig == null && norm == null && (translation == null || translation.getKey() == null) && lang == null;
    }

    private boolean isOrigEmpty(){
        return StringUtils.isEmpty(orig);
    }

    private boolean isLocalizationKeyEmpty(){
        return translation == null || StringUtils.isEmpty(translation.getKey());
    }

    private boolean isLanguageMapEmpty(){
        return lang == null || lang.isEmpty();
    }

    public void recompute(PolyStringNormalizer normalizer) {
        norm = normalizer.normalize(orig);
    }

    public boolean isComputed() {
        return !(norm == null);
    }

    @Override
    public Object resolve(ItemPath subpath) {
        if (subpath == null || subpath.isEmpty()) {
            return this;
        }
        if (subpath.size() > 1) {
            throw new IllegalArgumentException("Cannot resolve path "+subpath+" on polystring "+this+", the path is too deep");
        }
        Object first = subpath.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Cannot resolve non-name path "+subpath+" on polystring "+this);
        }
        QName itemName = ItemPath.toName(first);
        if (QNameUtil.match(F_ORIG, itemName)) {
            return orig;
        } else if (QNameUtil.match(F_NORM, itemName)) {
            return norm;
        } else if (QNameUtil.match(F_TRANSLATION, itemName)) {
            return translation;
        } else if (QNameUtil.match(F_LANG, itemName)) {
            return lang;
        } else {
            throw new IllegalArgumentException("Unknown path segment "+itemName);
        }
    }

    // Groovy operator overload
    public PolyString plus(Object other) {
        if (other == null) {
            return this;
        }
        return new PolyString(this.orig + other.toString());
    }

    // Groovy operator overload
    public PolyString getAt(int index) {
        return new PolyString(this.orig.substring(index, index+1));
    }

    @Override
    public int compareTo(Object other) {
        if (other == null) {
            return 1;
        }
        String otherString = other.toString();
        return this.orig.compareTo(otherString);
    }

//    public PolyString getAt(Range at) {
//        // TODO
//    }
//
//    public PolyString getAt(IntRange at) {
//        // TODO
//    }

    public int length() {
        return orig.length();
    }

    public PolyString trim() {
        return new PolyString(orig.trim(), norm != null ? norm.trim() : norm);
    }

    public String substring(int from, int to) {
        return this.orig.substring(from,to);
    }

    /**
     * Helper function that checks whether this original string begins with the specified value.
     *
     * @param value the value
     * @return the string
     */
    public boolean startsWith(String value) {
        return this.orig.startsWith(value);
    }

    /**
     * Helper function that checks whether this original string ends with the specified value.
     *
     * @param value the value
     * @return the string
     */
    public boolean endsWith(String value) {
        return this.orig.endsWith(value);
    }

    // Do NOT auto-generate this: there are manual changes!
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lang == null || lang.isEmpty()) ? 0 : lang.hashCode());
        result = prime * result + ((norm == null) ? 0 : norm.hashCode());
        result = prime * result + ((orig == null) ? 0 : orig.hashCode());
        result = prime * result + ((translation == null) ? 0 : translation.hashCode());
        return result;
    }

    // Do NOT auto-generate this: there are manual changes!
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PolyString other = (PolyString) obj;
        if (lang == null || lang.isEmpty()) {
            if (other.lang != null && !other.lang.isEmpty()) {
                return false;
            }
        } else if (!lang.equals(other.lang)) {
            return false;
        }
        if (norm == null) {
            if (other.norm != null) {
                return false;
            }
        } else if (!norm.equals(other.norm)) {
            return false;
        }
        if (orig == null) {
            if (other.orig != null) {
                return false;
            }
        } else if (!orig.equals(other.orig)) {
            return false;
        }
        if (translation == null) {
            if (other.translation != null) {
                return false;
            }
        } else if (!translation.equals(other.translation)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equalsOriginalValue(Recomputable obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PolyString other = (PolyString) obj;
        if (orig == null) {
            if (other.orig != null) return false;
        } else if (!orig.equals(other.orig)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return orig;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PolyString(");
        sb.append(orig);
        if (norm != null) {
            sb.append(",");
            sb.append(norm);
        }
        if (translation != null) {
            sb.append(";translation=");
            sb.append(translation.getKey());
        }
        if (lang != null) {
            sb.append(";lang=");
            sb.append(lang);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (MapUtils.isNotEmpty(getLang()) || getTranslation() != null && StringUtils.isNotEmpty(getTranslation().getKey())){
            sb.append("orig=" + orig);
        } else {
            sb.append(orig);
        }
        if (getTranslation() != null) {
            sb.append("; translation.key=" + getTranslation().getKey());
        }
        if (MapUtils.isNotEmpty(getLang())) {
            sb.append("; lang:");
            getLang().keySet().forEach(langKey -> {
                sb.append(" " + langKey + "=" + getLang().get(langKey) + ",");
            });
        }
    }

    public static String getOrig(PolyString s) {
        return s != null ? s.getOrig() : null;
    }

    public static String getOrig(PolyStringType s) {
        return s != null ? s.getOrig() : null;
    }

    @Override
    public boolean match(PolyString other) {
        if (this == other) return true;
        if (other == null) return false;
        if (norm == null) {
            if (other.norm != null) return false;
        } else if (!norm.equals(other.norm)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(String regex) {
        return Pattern.matches(regex, norm) || Pattern.matches(regex, orig);
    }

    /**
     * Returns true if the PolyString form contains only simple string.
     * I.e. returns true if the polystring can be serialized in a simplified form of a single string.
     * Returns true in case that there are language mutations, translation, etc.
     */
    public boolean isSimple() {
        return translation == null && lang == null;
    }

    @Override
    public void checkConsistence() {
        if (orig == null) {
            throw new IllegalStateException("Null orig");
        }
        if (norm == null) {
            throw new IllegalStateException("Null norm");
        }
    }

    public static PolyString toPolyString(PolyStringType value) {
        return value != null ? value.toPolyString() : null;
    }

    public static PolyStringType toPolyStringType(PolyString value) {
        return value != null ? new PolyStringType(value) : null;
    }

    public static PolyString fromOrig(String orig) {
        return new PolyString(orig);
    }

}
