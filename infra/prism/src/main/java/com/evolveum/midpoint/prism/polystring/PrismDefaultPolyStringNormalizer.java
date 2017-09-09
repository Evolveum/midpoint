/*
 * Copyright (c) 2010-2015 Evolveum
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

/**
 * @author semancik
 *
 */
public class PrismDefaultPolyStringNormalizer implements PolyStringNormalizer {
	private static final String MALFORMED_REGEX = "[^\\p{L}\\w\\s\\d]";
	private static final Pattern MALFORMED_PATTERN = Pattern.compile(MALFORMED_REGEX);
	private static final String WHITESPACE_REGEX = "\\s+";
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile(WHITESPACE_REGEX);

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.polystring.PolyStringNormalizer#normalize(java.lang.String)
	 */
	@Override
	public String normalize(String orig) {
		if (orig == null) {
			return null;
		}
		String s = StringUtils.trim(orig);
		s = Normalizer.normalize(s, Normalizer.Form.NFKD);
		s = MALFORMED_PATTERN.matcher(s).replaceAll("");
		s = WHITESPACE_PATTERN.matcher(s).replaceAll(" ");
		if (StringUtils.isBlank(s)) {
			s = "";
		}
		return StringUtils.lowerCase(s);
	}

}
