/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.task.api;

/**
 * @author semancik
 *
 */
public class LightweightIdentifier {
	
	private static final String SEPARATOR = "-";
	
	private long timestamp;
	private int hostIdentifier;
	private int sequenceNumber;
	private String string;
	
	public LightweightIdentifier(long timestamp, int hostIdentifier, int sequenceNumber) {
		this.timestamp = timestamp;
		this.hostIdentifier = hostIdentifier;
		this.sequenceNumber = sequenceNumber;
		formatString();
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public int getHostIdentifier() {
		return hostIdentifier;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	private void formatString() {
		StringBuilder sb = new StringBuilder();
		sb.append(timestamp);
		sb.append(SEPARATOR);
		sb.append(hostIdentifier);
		sb.append(SEPARATOR);
		sb.append(sequenceNumber);
		string = sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LightweightIdentifier other = (LightweightIdentifier) obj;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return string;
	}
	
}
