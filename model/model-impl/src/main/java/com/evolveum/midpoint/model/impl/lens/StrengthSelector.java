/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

/**
 * @author semancik
 *
 */
public class StrengthSelector {
	
	public static final StrengthSelector ALL = new StrengthSelector(true, true, true);
	public static final StrengthSelector ALL_EXCEPT_WEAK = new StrengthSelector(false, true, true);
	public static final StrengthSelector WEAK_ONLY = new StrengthSelector(true, false, false);
	
	private final boolean weak;
	private final boolean normal;
	private final boolean strong;
	
	public StrengthSelector(boolean weak, boolean normal, boolean strong) {
		super();
		this.weak = weak;
		this.normal = normal;
		this.strong = strong;
	}

	public boolean isWeak() {
		return weak;
	}

	public boolean isNormal() {
		return normal;
	}

	public boolean isStrong() {
		return strong;
	}
	
	public StrengthSelector notWeak() {
		return new StrengthSelector(false, normal, strong);
	}
	
	public boolean isNone() {
		return !weak && !normal && !strong;
	}

	@Override
	public String toString() {
		return "StrengthSelector(weak=" + weak + ", normal=" + normal + ", strong=" + strong + ")";
	}
}
