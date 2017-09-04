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
package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * This exception should be thrown if there is no other practical way how to
 * determine the problem.
 *
 * @author Radovan Semancik
 */
public class GenericFrameworkException extends UcfException {

	/**
	 * Creates a new instance of <code>GenericFrameworkException</code> without detail message.
	 */
	public GenericFrameworkException() {
	}

	/**
	 * Constructs an instance of <code>GenericFrameworkException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public GenericFrameworkException(String msg) {
		super(msg);
	}

	public GenericFrameworkException(Exception ex) {
		super(ex);
	}

	public GenericFrameworkException(String msg, Exception ex) {
		super(msg,ex);
	}

}
