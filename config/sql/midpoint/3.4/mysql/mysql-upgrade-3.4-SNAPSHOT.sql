ALTER TABLE m_acc_cert_case ADD overallOutcome INTEGER;
ALTER TABLE m_acc_cert_case CHANGE currentResponse currentStageOutcome INTEGER;
