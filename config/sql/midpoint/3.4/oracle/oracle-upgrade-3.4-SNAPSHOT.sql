ALTER TABLE m_acc_cert_case ADD overallOutcome NUMBER(10, 0);
ALTER TABLE m_acc_cert_case RENAME COLUMN currentResponse TO currentStageOutcome;
