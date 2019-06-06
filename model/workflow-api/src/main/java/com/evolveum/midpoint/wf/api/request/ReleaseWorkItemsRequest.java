/*
 * Copyright (c) 2010-2019 Evolveum
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
}
