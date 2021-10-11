/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.polystring;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import org.apache.commons.lang.StringUtils;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

public abstract class AbstractPolyStringNormalizer implements PolyStringNormalizer, ConfigurableNormalizer {

    private static final String WHITESPACE_REGEX = "\\s+";
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(WHITESPACE_REGEX);

    private PolyStringNormalizerConfigurationType configuration;

    @Override
    public void configure(PolyStringNormalizerConfigurationType configuration) {
        this.configuration = configuration;
    }

    protected PolyStringNormalizerConfigurationType getConfiguration() {
        return configuration;
    }

    protected String trim(String s) {
        return StringUtils.trim(s);
    }

    /**
     * Unicode Normalization Form Compatibility Decomposition (NFKD)
     */
    protected String nfkd(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKD);
    }

    protected String replaceAll(String s, Pattern pattern, String replacement) {
        return pattern.matcher(s).replaceAll(replacement);
    }

    protected String removeAll(String s, Pattern pattern) {
        return pattern.matcher(s).replaceAll("");
    }

    // TODO: Should be named keepOnly
    protected String removeAll(String s, int lowerCode, int upperCode) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= lowerCode && c <= upperCode) {
                out.append(c);
            }
        }
        return out.toString();
    }

    protected String trimWhitespace(String s) {
        return replaceAll(s, WHITESPACE_PATTERN, " ");
    }

    protected String lowerCase(String s) {
        return StringUtils.lowerCase(s);
    }

    protected boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.polystring.PolyStringNormalizer#normalize(java.lang.String)
     */
    @Override
    public String normalize(String orig) {
        if (orig == null) {
            return null;
        }
        String s = preprocess(orig);

        s = normalizeCore(s);

        return postprocess(s);
    }

    protected abstract String normalizeCore(String s);

    protected String preprocess(String s) {
        if (configuration == null || !Boolean.FALSE.equals(configuration.isTrim())) {
            s = trim(s);
        }

        if (configuration == null || !Boolean.FALSE.equals(configuration.isNfkd())) {
            s = nfkd(s);
        }
        return s;
    }

    protected String postprocess(String s) {
        if (configuration == null || !Boolean.FALSE.equals(configuration.isTrimWhitespace())) {
            s = trimWhitespace(s);
            if (isBlank(s)) {
                return "";
            }
        }

        if (configuration == null || !Boolean.FALSE.equals(configuration.isLowercase())) {
            s = lowerCase(s);
        }

        return s;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("(");
        if (configuration != null) {
            configuration.shortDump(sb);
        }
        sb.append(")");
        return sb.toString();
    }

}
