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
package com.evolveum.midpoint.prism.delta;

import java.util.function.Supplier;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Foreachable;
import com.evolveum.midpoint.util.Processor;

/**
 * Utility class for keeping things in three: plus, zero and minus.
 *
 * @author semancik
 */
public class DeltaTriple<T> implements DebugDumpable, Foreachable<T> {

	private T plus;
	private T zero;
	private T minus;

	public DeltaTriple() {
		plus = null;
		zero = null;
		minus = null;
	}

	public DeltaTriple(Supplier<T> initializer) {
		plus = initializer.get();
		zero = initializer.get();
		minus = initializer.get();
	}

	public T getPlus() {
		return plus;
	}

	public void setPlus(T plus) {
		this.plus = plus;
	}

	public T getZero() {
		return zero;
	}

	public void setZero(T zero) {
		this.zero = zero;
	}

	public T getMinus() {
		return minus;
	}

	public void setMinus(T minus) {
		this.minus = minus;
	}

	public T get(PlusMinusZero mode) {
		switch (mode) {
			case PLUS: return plus;
			case ZERO: return zero;
			case MINUS: return minus;
		}
		throw new IllegalArgumentException("Wrong mode "+mode);
	}

	@Override
	public void foreach(Processor<T> processor) {
		processor.process(plus);
		processor.process(zero);
		processor.process(minus);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilder(DeltaTriple.class, indent);
		sb.append("\n");
		debugDumpNoTitle(sb, indent);
		return sb.toString();
	}

	public String debugDumpNoTitle(StringBuilder sb, int indent) {
		debugDumpVal(sb, plus, "plus", indent);
		sb.append("\n");
		debugDumpVal(sb, zero, "zero", indent);
		sb.append("\n");
		debugDumpVal(sb, minus, "minus", indent);
		return sb.toString();
	}

	private void debugDumpVal(StringBuilder sb, T val, String label, int indent) {
		if (val == null || !(val instanceof DebugDumpable)) {
			DebugUtil.debugDumpWithLabelToString(sb, label, val, indent + 1);
		} else {
			DebugUtil.debugDumpWithLabel(sb, label, (DebugDumpable)val, indent + 1);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((minus == null) ? 0 : minus.hashCode());
		result = prime * result + ((plus == null) ? 0 : plus.hashCode());
		result = prime * result + ((zero == null) ? 0 : zero.hashCode());
		return result;
	}

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
		DeltaTriple other = (DeltaTriple) obj;
		if (minus == null) {
			if (other.minus != null) {
				return false;
			}
		} else if (!minus.equals(other.minus)) {
			return false;
		}
		if (plus == null) {
			if (other.plus != null) {
				return false;
			}
		} else if (!plus.equals(other.plus)) {
			return false;
		}
		if (zero == null) {
			if (other.zero != null) {
				return false;
			}
		} else if (!zero.equals(other.zero)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DeltaTriple(plus=" + plus + ", zero=" + zero + ", minus=" + minus + ")";
	}

}
