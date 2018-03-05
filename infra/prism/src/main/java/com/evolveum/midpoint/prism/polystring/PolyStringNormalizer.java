/*
 * Copyright (c) 2010-2013 Evolveum
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

/**
 * Normalizer for PolyStrings. Classes implementing this interface are able to take an
 * original string (in the PolyString sense) and return a normalized version of the
 * string.
 *
 * @see PolyString
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface PolyStringNormalizer {

	/**
	 * Returns a normalized version of the string.
	 * @return normalized version of the string
	 */
	String normalize(String orig);

}
