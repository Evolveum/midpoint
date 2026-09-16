-- Thanks to Patrick Lightbody for submitting this.

-- Developer documentation for SQL documentation annotations:
-- https://docs.evolveum.com/midpoint/devel/guides/sql-script-annotations/

drop table if exists qrtz_fired_triggers;
DROP TABLE if exists QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE if exists QRTZ_SCHEDULER_STATE;
DROP TABLE if exists QRTZ_LOCKS;
drop table if exists qrtz_simple_triggers;
drop table if exists qrtz_cron_triggers;
drop table if exists qrtz_simprop_triggers;
DROP TABLE if exists QRTZ_BLOB_TRIGGERS;
drop table if exists qrtz_triggers;
drop table if exists qrtz_job_details;
drop table if exists qrtz_calendars;

-- @description: Stores Quartz scheduler job definitions used by midPoint task scheduling.
CREATE TABLE qrtz_job_details (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Job name assigned by Quartz.
    JOB_NAME VARCHAR(200) NOT NULL,
    -- @description: Job group assigned by Quartz.
    JOB_GROUP VARCHAR(200) NOT NULL,
    -- @description: Optional human-readable job description.
    DESCRIPTION VARCHAR(250) NULL,
    -- @description: Java class implementing the scheduled job.
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    -- @description: Indicates whether the job should remain stored even when it has no triggers.
    IS_DURABLE BOOL NOT NULL,
    -- @description: Indicates whether Quartz must avoid concurrent execution of this job.
    IS_NONCONCURRENT BOOL NOT NULL,
    -- @description: Indicates whether job data should be updated after execution.
    IS_UPDATE_DATA BOOL NOT NULL,
    -- @description: Indicates whether the job requests recovery after scheduler failure.
    REQUESTS_RECOVERY BOOL NOT NULL,
    -- @description: Serialized Quartz job data.
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

-- @description: Stores Quartz trigger definitions that decide when jobs should run.
CREATE TABLE qrtz_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Trigger name assigned by Quartz.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Trigger group assigned by Quartz.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: Name of the job fired by this trigger.
    JOB_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the job fired by this trigger.
    JOB_GROUP VARCHAR(200) NOT NULL,
    -- @description: Optional human-readable trigger description.
    DESCRIPTION VARCHAR(250) NULL,
    -- @description: Next scheduled fire time stored as a timestamp value used by Quartz.
    NEXT_FIRE_TIME BIGINT NULL,
    -- @description: Previous fire time stored as a timestamp value used by Quartz.
    PREV_FIRE_TIME BIGINT NULL,
    -- @description: Trigger priority used by Quartz when multiple triggers are ready.
    PRIORITY INTEGER NULL,
    -- @description: Execution group used by Quartz for grouped trigger execution.
    EXECUTION_GROUP VARCHAR(200) NULL,
    -- @description: Current trigger state, for example waiting, paused, or acquired.
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    -- @description: Trigger type, such as simple, cron, blob, or simulated-property trigger.
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    -- @description: Start time of the trigger.
    START_TIME BIGINT NOT NULL,
    -- @description: Optional end time of the trigger.
    END_TIME BIGINT NULL,
    -- @description: Calendar name associated with this trigger, if any.
    CALENDAR_NAME VARCHAR(200) NULL,
    -- @description: Misfire instruction used when a scheduled fire time was missed.
    MISFIRE_INSTR SMALLINT NULL,
    -- @description: Serialized Quartz trigger data.
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
);

-- @description: Stores additional data for Quartz simple triggers.
CREATE TABLE qrtz_simple_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Name of the related trigger.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the related trigger.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: Number of times the trigger should repeat.
    REPEAT_COUNT BIGINT NOT NULL,
    -- @description: Interval between repeated executions.
    REPEAT_INTERVAL BIGINT NOT NULL,
    -- @description: Number of times the trigger has already fired.
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

-- @description: Stores additional data for Quartz cron triggers.
CREATE TABLE qrtz_cron_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Name of the related trigger.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the related trigger.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: Cron expression defining when the trigger fires.
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    -- @description: Time zone used to evaluate the cron expression.
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

-- @description: Stores additional string, numeric, decimal, and boolean properties for Quartz triggers.
CREATE TABLE qrtz_simprop_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Name of the related trigger.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the related trigger.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: First string property stored by Quartz for this trigger.
    STR_PROP_1 VARCHAR(512) NULL,
    -- @description: Second string property stored by Quartz for this trigger.
    STR_PROP_2 VARCHAR(512) NULL,
    -- @description: Third string property stored by Quartz for this trigger.
    STR_PROP_3 VARCHAR(512) NULL,
    -- @description: First integer property stored by Quartz for this trigger.
    INT_PROP_1 INT NULL,
    -- @description: Second integer property stored by Quartz for this trigger.
    INT_PROP_2 INT NULL,
    -- @description: First long integer property stored by Quartz for this trigger.
    LONG_PROP_1 BIGINT NULL,
    -- @description: Second long integer property stored by Quartz for this trigger.
    LONG_PROP_2 BIGINT NULL,
    -- @description: First decimal property stored by Quartz for this trigger.
    DEC_PROP_1 NUMERIC(13, 4) NULL,
    -- @description: Second decimal property stored by Quartz for this trigger.
    DEC_PROP_2 NUMERIC(13, 4) NULL,
    -- @description: First boolean property stored by Quartz for this trigger.
    BOOL_PROP_1 BOOL NULL,
    -- @description: Second boolean property stored by Quartz for this trigger.
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

-- @description: Stores serialized binary data for Quartz blob triggers.
CREATE TABLE qrtz_blob_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Name of the related trigger.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the related trigger.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: Serialized blob trigger data.
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

-- @description: Stores Quartz calendar definitions used by triggers.
CREATE TABLE qrtz_calendars (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Calendar name assigned by Quartz.
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    -- @description: Serialized Quartz calendar data.
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

-- @description: Stores Quartz trigger groups that are currently paused.
CREATE TABLE qrtz_paused_trigger_grps (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Paused trigger group name.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

-- @description: Stores currently fired Quartz trigger instances.
CREATE TABLE qrtz_fired_triggers (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Unique fired-trigger entry identifier.
    ENTRY_ID VARCHAR(95) NOT NULL,
    -- @description: Name of the fired trigger.
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    -- @description: Group of the fired trigger.
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    -- @description: Scheduler instance that fired the trigger.
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    -- @description: Time when the trigger fired.
    FIRED_TIME BIGINT NOT NULL,
    -- @description: Scheduled fire time.
    SCHED_TIME BIGINT NOT NULL,
    -- @description: Trigger priority at fire time.
    PRIORITY INTEGER NOT NULL,
    -- @description: Execution group used for this fired trigger.
    EXECUTION_GROUP VARCHAR(200) NULL,
    -- @description: State of the fired trigger entry.
    STATE VARCHAR(16) NOT NULL,
    -- @description: Job name associated with this fired trigger, if any.
    JOB_NAME VARCHAR(200) NULL,
    -- @description: Job group associated with this fired trigger, if any.
    JOB_GROUP VARCHAR(200) NULL,
    -- @description: Indicates whether the associated job disallows concurrent execution.
    IS_NONCONCURRENT BOOL NULL,
    -- @description: Indicates whether the associated job requests recovery.
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

-- @description: Stores Quartz scheduler instances and their last check-in times.
CREATE TABLE qrtz_scheduler_state (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Scheduler node or instance name.
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    -- @description: Last check-in time of the scheduler instance.
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    -- @description: Expected check-in interval for this scheduler instance.
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

-- @description: Stores Quartz scheduler lock records used for cluster coordination.
CREATE TABLE qrtz_locks (
    -- @description: Scheduler instance name.
    SCHED_NAME VARCHAR(120) NOT NULL,
    -- @description: Lock name used by Quartz.
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

-- @description: Speeds up lookup of jobs that request recovery.
-- @usedFor: Quartz recovery processing
create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME, REQUESTS_RECOVERY);

-- @description: Speeds up lookup of Quartz jobs by group.
-- @usedFor: scheduler job group lookup
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME, JOB_GROUP);

-- @description: Speeds up lookup of triggers by related job.
-- @usedFor: resolving triggers for a Quartz job
create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME, JOB_NAME, JOB_GROUP);

-- @description: Speeds up lookup of triggers by related job group.
-- @usedFor: scheduler trigger lookup by job group
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME, JOB_GROUP);

-- @description: Speeds up lookup of triggers by calendar name.
-- @usedFor: finding triggers attached to a Quartz calendar
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME, CALENDAR_NAME);

-- @description: Speeds up lookup of triggers by trigger group.
-- @usedFor: scheduler trigger group lookup
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME, TRIGGER_GROUP);

-- @description: Speeds up lookup of triggers by trigger state.
-- @usedFor: scheduler trigger state lookup
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME, TRIGGER_STATE);

-- @description: Speeds up lookup of one trigger together with its state.
-- @usedFor: trigger state lookup by trigger identity
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);

-- @description: Speeds up lookup of triggers by group and state.
-- @usedFor: trigger group state lookup
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);

-- @description: Speeds up lookup of triggers by next fire time.
-- @usedFor: finding triggers ready to fire
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME, NEXT_FIRE_TIME);

-- @description: Speeds up lookup of triggers by state and next fire time.
-- @usedFor: finding active triggers ready to fire
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);

-- @description: Speeds up lookup of misfired triggers by next fire time.
-- @usedFor: Quartz misfire handling
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);

-- @description: Speeds up lookup of misfired triggers by state and next fire time.
-- @usedFor: Quartz misfire handling with trigger state filtering
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);

-- @description: Speeds up lookup of misfired triggers by group, state, and next fire time.
-- @usedFor: Quartz misfire handling with trigger group filtering
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

-- @description: Speeds up lookup of fired triggers by scheduler instance.
-- @usedFor: scheduler instance recovery and fired-trigger tracking
create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME, INSTANCE_NAME);

-- @description: Speeds up lookup of fired triggers by instance and recovery flag.
-- @usedFor: recovery processing for fired jobs
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);

-- @description: Speeds up lookup of fired triggers by job.
-- @usedFor: fired-trigger lookup by job identity
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME, JOB_NAME, JOB_GROUP);

-- @description: Speeds up lookup of fired triggers by job group.
-- @usedFor: fired-trigger lookup by job group
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME, JOB_GROUP);

-- @description: Speeds up lookup of fired triggers by trigger identity.
-- @usedFor: fired-trigger lookup by trigger name and group
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

-- @description: Speeds up lookup of fired triggers by trigger group.
-- @usedFor: fired-trigger lookup by trigger group
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME, TRIGGER_GROUP);
