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
package com.evolveum.midpoint.test.util;

/**
 * @author semancik
 *
 */
public class Counter {
	private int count = 0;

	public int getCount() {
		return count;
	}

	public synchronized void click() {
		count++;
	}

	public void assertCount(int expectedCount) {
		assert count == expectedCount : "Wrong counter, expected "+expectedCount+", was "+count;
	}

	public void assertCount(String message, int expectedCount) {
		assert count == expectedCount : message + ", expected "+expectedCount+", was "+count;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
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
		Counter other = (Counter) obj;
		if (count != other.count) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Counter(" + count + ")";
	}


}
