/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.polystring;

import java.text.Normalizer;
import java.util.regex.Pattern;

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
	
	protected String removeAll(String s, int lowerCode, int upperCode) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= lowerCode || c <= upperCode) {
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
