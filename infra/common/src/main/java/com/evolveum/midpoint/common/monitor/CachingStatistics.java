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
package com.evolveum.midpoint.common.monitor;

/**
 * @author semancik
 *
 */
public class CachingStatistics {
	
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

}
