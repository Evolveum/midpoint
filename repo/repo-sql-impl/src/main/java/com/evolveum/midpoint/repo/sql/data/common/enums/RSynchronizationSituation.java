package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

public enum RSynchronizationSituation {
	
	DELETED(SynchronizationSituationType.DELETED),
	UNMATCHED(SynchronizationSituationType.UNMATCHED),
	DISPUTED(SynchronizationSituationType.DISPUTED),
	LINKED(SynchronizationSituationType.LINKED),
	UNLINKED(SynchronizationSituationType.UNLINKED);
	
	private SynchronizationSituationType syncType;
	
	private RSynchronizationSituation(SynchronizationSituationType syncType){
		this.syncType = syncType;
	}
	
	public SynchronizationSituationType getSyncType() {
		return syncType;
	}

	public static RSynchronizationSituation toRepoType(SynchronizationSituationType syncType){
		
		if (syncType == null){
			return null;
		}
		
		for (RSynchronizationSituation repo : RSynchronizationSituation.values()){
			if (repo.getSyncType().equals(syncType)){
				return repo;
			}
		}
		
		throw new IllegalArgumentException("Unknown synchronization situation type " + syncType);
	}
	
}
