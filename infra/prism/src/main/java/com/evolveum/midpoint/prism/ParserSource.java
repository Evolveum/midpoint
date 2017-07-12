/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Source for prism parser (file, input stream, string, DOM tree, ...).
 *
 * @author mederly
 */
public interface ParserSource {

	/**
	 * Presents the input data in the form of an InputStream.
	 * For some special cases might not be implemented, and the data could be accessed in another way.
	 */
	@NotNull
	InputStream getInputStream() throws IOException;

	/**
	 * Should the stream be closed after parsing? Useful for sources that create/open the stream themselves.
	 */
	boolean closeStreamAfterParsing();

	/**
	 * Is the source expected to throw IOExceptions?
	 */
	boolean throwsIOException();
}
