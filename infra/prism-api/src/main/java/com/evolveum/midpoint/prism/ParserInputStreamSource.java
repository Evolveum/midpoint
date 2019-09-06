/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * @author mederly
 */
public class ParserInputStreamSource implements ParserSource {

	@NotNull private final InputStream inputStream;

	public ParserInputStreamSource(@NotNull InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@NotNull
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public boolean closeStreamAfterParsing() {
		return false;		// TODO eventually make configurable
	}

	@Override
	public boolean throwsIOException() {
		return true;
	}
}
