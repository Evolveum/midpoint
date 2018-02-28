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
package com.evolveum.midpoint.schema.internals;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class CachingStatistics implements DebugDumpable {

	private long requests = 0;
	private long hits = 0;
	private long misses = 0;

	public long getRequests() {
		return requests;
	}

	public synchronized void setRequests(long requests) {
		this.requests = requests;
	}

	public synchronized void recordRequest() {
		this.requests++;
	}

	public long getHits() {
		return hits;
	}

	public synchronized void setHits(long hits) {
		this.hits = hits;
	}

	public synchronized void recordHit() {
		this.hits++;
	}

	public long getMisses() {
		return misses;
	}

	public synchronized void setMisses(long misses) {
		this.misses = misses;
	}

	public synchronized void recordMiss() {
		this.misses++;
	}

	public CachingStatistics clone() {
		CachingStatistics clone = new CachingStatistics();
		clone.requests = this.requests;
		clone.hits = this.hits;
		clone.misses = this.misses;
		return clone;
	}

	@Override
	public String toString() {
		return "CachingStatistics(requests=" + requests + ", hits=" + hits + ", misses=" + misses + ")";
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CachingStatistics.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "requests", requests, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "hits", hits, indent);
		DebugUtil.debugDumpWithLabel(sb, "misses", misses, indent);
		return sb.toString();
	}

}
