/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api.request;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class ReleaseWorkItemsRequest extends Request {

	public static class SingleRelease {
		private final long workItemId;

		public SingleRelease(long workItemId) {
			this.workItemId = workItemId;
		}

		public long getWorkItemId() {
			return workItemId;
		}

		@Override
		public String toString() {
			return "SingleRelease{" +
					"workItemId=" + workItemId +
					'}';
		}
	}

	@NotNull private final Collection<SingleRelease> releases = new ArrayList<>();

	public ReleaseWorkItemsRequest(@NotNull String caseOid) {
		super(caseOid, null);
	}

	@NotNull
	public Collection<SingleRelease> getReleases() {
		return releases;
	}

	@Override
	public String toString() {
		return "ReleaseWorkItemsRequest{" +
				"releases=" + releases +
				", caseOid='" + caseOid + '\'' +
				", causeInformation=" + causeInformation +
				'}';
	}
}
