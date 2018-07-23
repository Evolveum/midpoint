/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.test.asserter;

import org.testng.AssertJUnit;

/**
 * @author semancik
 *
 */
public abstract class AbstractAsserter<R> {
	
	private String details;
	private R returnAsserter;
	
	public AbstractAsserter() {
		this(null);
	}
	
	public AbstractAsserter(String details) {
		super();
		this.details = details;
	}
	
	public AbstractAsserter(R returnAsserter, String details) {
		super();
		this.returnAsserter = returnAsserter;
		this.details = details;
	}

	protected String getDetails() {
		return details;
	}

	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

	protected String descWithDetails(Object o) {
		if (details == null) {
			return o.toString();
		} else {
			return o.toString()+" ("+details+")";
		}
	}
	
	public R end() {
		return returnAsserter;
	}
}
