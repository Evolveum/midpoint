ALTER TABLE m_acc_cert_case ADD overallOutcome INT4;
ALTER TABLE m_acc_cert_case RENAME COLUMN currentResponse TO currentStageOutcome;
