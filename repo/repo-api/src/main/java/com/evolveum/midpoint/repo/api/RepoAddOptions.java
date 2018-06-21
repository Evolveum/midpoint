/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.repo.api;

import java.io.Serializable;

import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 *
 */
public class RepoAddOptions extends AbstractOptions implements Serializable, ShortDumpable {
	private static final long serialVersionUID = -6243926109579064467L;

	private boolean overwrite = false;
	private boolean allowUnencryptedValues = false;

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public static boolean isOverwrite(RepoAddOptions options) {
		if (options == null) {
			return false;
		}
		return options.isOverwrite();
	}

	public static RepoAddOptions createOverwrite() {
		RepoAddOptions opts = new RepoAddOptions();
		opts.setOverwrite(true);
		return opts;
	}


	public boolean isAllowUnencryptedValues() {
		return allowUnencryptedValues;
	}

	public void setAllowUnencryptedValues(boolean allowUnencryptedValues) {
		this.allowUnencryptedValues = allowUnencryptedValues;
	}

	public static boolean isAllowUnencryptedValues(RepoAddOptions options) {
		if (options == null) {
			return false;
		}
		return options.isAllowUnencryptedValues();
	}

	public static RepoAddOptions createAllowUnencryptedValues() {
		RepoAddOptions opts = new RepoAddOptions();
		opts.setAllowUnencryptedValues(true);
		return opts;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RepoAddOptions(");
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void shortDump(StringBuilder sb) {
		appendFlag(sb, "overwrite", overwrite);
		appendFlag(sb, "allowUnencryptedValues", allowUnencryptedValues);
		removeLastComma(sb);
	}

}
