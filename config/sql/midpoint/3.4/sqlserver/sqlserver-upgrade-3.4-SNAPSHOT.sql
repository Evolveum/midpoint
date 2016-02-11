ALTER TABLE m_acc_cert_case ADD overallOutcome INT;
EXEC sp_rename 'm_acc_cert_case.currentResponse', 'currentStageOutcome', 'COLUMN';
