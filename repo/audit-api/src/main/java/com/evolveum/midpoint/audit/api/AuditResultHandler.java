package com.evolveum.midpoint.audit.api;

public interface AuditResultHandler {


	public boolean handle(AuditEventRecord auditRecord);

	public int getProgress();
}
