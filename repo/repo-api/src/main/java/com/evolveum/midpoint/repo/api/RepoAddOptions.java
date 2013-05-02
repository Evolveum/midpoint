/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.repo.api;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class RepoAddOptions implements Serializable {	
	private static final long serialVersionUID = -6243926109579064467L;
	
	private boolean overwrite = false;

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public static RepoAddOptions createOverwrite() {
		RepoAddOptions opts = new RepoAddOptions();
		opts.setOverwrite(true);
		return opts;
	}

	@Override
	public String toString() {
		return "RepoAddOptions(overwrite=" + overwrite + ")";
	}

}
