/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.delta;

/**
 * Simple enumeration that refers to the plus, minus or zero concepts
 * used in delta set triples.
 *
 * @author Radovan Semancik
 *
 */
public enum PlusMinusZero {

	PLUS, MINUS, ZERO;

	public static PlusMinusZero compute(PlusMinusZero mode1, PlusMinusZero mode2) {
		if (mode1 == null || mode2 == null) {
			return null;
		}
		switch (mode1) {
			case PLUS: switch (mode2) {
				case PLUS: return PlusMinusZero.PLUS;
				case ZERO: return PlusMinusZero.PLUS;
				case MINUS: return null;
			}
			case ZERO: switch (mode2) {
				case PLUS: return PlusMinusZero.PLUS;
				case ZERO: return PlusMinusZero.ZERO;
				case MINUS: return PlusMinusZero.MINUS;
			}
			case MINUS: switch (mode2) {
				case PLUS: return null;
				case ZERO: return PlusMinusZero.MINUS;
				case MINUS: return PlusMinusZero.MINUS;
			}
		}
		// notreached
		throw new IllegalStateException();
	}
}
