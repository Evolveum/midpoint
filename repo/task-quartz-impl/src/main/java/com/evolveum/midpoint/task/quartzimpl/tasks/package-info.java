/**
 * Deals with tasks at high level:
 *
 * 1. retrieves the tasks - {@link com.evolveum.midpoint.task.quartzimpl.tasks.TaskRetriever};
 * 2. manages the task state i.e. suspends, resumes, runs immediately, closes, deletes them -
 * {@link com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager} and its helper classes;
 * 3. instantiates the tasks - {@link com.evolveum.midpoint.task.quartzimpl.tasks.TaskInstantiator};
 * 4. persists the tasks - {@link com.evolveum.midpoint.task.quartzimpl.tasks.TaskPersister};
 * 5. cleans up obsolete tasks - {@link com.evolveum.midpoint.task.quartzimpl.tasks.TaskCleaner}.
 *
 * Does NOT:
 *
 * 1. does not deal with scheduling directly. This is done in ... package;
 * 2. does not manage the nodes and the cluster. This is done in ... package.
 */
package com.evolveum.midpoint.task.quartzimpl.tasks;
