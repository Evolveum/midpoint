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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.exception.TunnelException;

/**
 * @author Radovan Semancik
 *
 */
public class ExceptionUtil {
	
	public static Throwable lookForTunneledException(Throwable ex) {
		if (ex instanceof TunnelException) {
			return ex.getCause();
		}
		if (ex.getCause() != null) {
			return lookForTunneledException(ex.getCause());
		}
		return null;
	}

	public static String lookForMessage(Throwable e) {
		if (e.getMessage() != null) {
			return e.getMessage();
		}
		if (e.getCause() != null) {
			return lookForMessage(e.getCause());
		}
		return null;
	}
	
}
