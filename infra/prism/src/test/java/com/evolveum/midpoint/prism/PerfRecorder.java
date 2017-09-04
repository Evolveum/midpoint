/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.prism;

import org.testng.AssertJUnit;

/**
 * @author semancik
 *
 */
public class PerfRecorder {

	private String name;
	private int count = 0;
	private Double min = null;
	private Double max = null;
	private Double sum = 0D;

	public PerfRecorder(String name) {
		super();
		this.name = name;
	}

	public void record(int index, Double value) {
		sum += value;
		count ++;
		if (min == null || value < min) {
			min = value;
		}
		if (max == null || value > max) {
			max = value;
		}
	}

	public int getCount() {
		return count;
	}

	public Double getMin() {
		return min;
	}

	public Double getMax() {
		return max;
	}

	public Double getSum() {
		return sum;
	}

	public double getAverage() {
		return sum/count;
	}

	public void assertAverageBelow(double expected) {
		AssertJUnit.assertTrue(name+ ": Expected average below "+expected+" but was "+getAverage(), getAverage() < expected);
	}

	public void assertMaxBelow(double expected) {
		AssertJUnit.assertTrue(name+ ": Expected maximum below "+expected+" but was "+max, max < expected);
	}


	public String dump() {
		return name + ": min / avg / max = "+min+" / "+getAverage()+" / "+max + " (sum="+sum+", count="+count+")";
	}

}
