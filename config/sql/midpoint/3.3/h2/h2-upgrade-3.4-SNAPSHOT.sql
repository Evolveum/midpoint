ALTER TABLE m_acc_cert_case ADD overallOutcome INTEGER;
ALTER TABLE m_acc_cert_case ALTER COLUMN currentResponse RENAME TO currentStageOutcome;
