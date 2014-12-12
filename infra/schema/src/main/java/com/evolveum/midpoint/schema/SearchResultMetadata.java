/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.schema;

/**
 * Objects of this type are considered READ ONLY.
 *
 * @author semancik
 */
public class SearchResultMetadata {
	
	private String pagingCookie;
	private Integer approxNumberOfAllResults;
	
	/**
	 * Returns the paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 */
	public String getPagingCookie() {
		return pagingCookie;
	}
	
	/**
	 * Sets paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 */
	public void setPagingCookie(String pagingCookie) {
		this.pagingCookie = pagingCookie;
	}
	
	/**
	 * Returns the approximate number of all results that would be returned for the
	 * filter if there was no paging limitation. This property is optional and it is
	 * informational only. The implementation should return it only if it is extremely
	 * cheap to get the information (e.g. if it is part of the response anyway).
	 * The number may be approximate. The intended use of this value is to optimize presentation
	 * of the data (e.g. to set approximate size of scroll bars, page counts, etc.)
	 */
	public Integer getApproxNumberOfAllResults() {
		return approxNumberOfAllResults;
	}
	
	public void setApproxNumberOfAllResults(Integer approxNumberOfAllResults) {
		this.approxNumberOfAllResults = approxNumberOfAllResults;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((approxNumberOfAllResults == null) ? 0 : approxNumberOfAllResults.hashCode());
		result = prime * result + ((pagingCookie == null) ? 0 : pagingCookie.hashCode());
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
		SearchResultMetadata other = (SearchResultMetadata) obj;
		if (approxNumberOfAllResults == null) {
			if (other.approxNumberOfAllResults != null)
				return false;
		} else if (!approxNumberOfAllResults.equals(other.approxNumberOfAllResults))
			return false;
		if (pagingCookie == null) {
			if (other.pagingCookie != null)
				return false;
		} else if (!pagingCookie.equals(other.pagingCookie))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SearchResultMetadata(pagingCookie=" + pagingCookie + ", approxNumberOfAllResults="
				+ approxNumberOfAllResults + ")";
	}

}
