package com.evolveum.midpoint.wf.api;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class Constants {
    public static final String WORKFLOW_EXTENSION_NS = "http://midpoint.evolveum.com/model/workflow/extension-2";
    public static final QName WFPROCESS_INSTANCE_FINISHED_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceFinished");
    public static final QName WFRESULTING_DELTA_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "resultingDelta");
    public static final QName WFDELTA_TO_PROCESS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "deltaToProcess");
    public static final QName WFPROCESSID_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceId");
    public static final QName WFCHANGE_PROCESSOR_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "changeProcessor");
    public static final QName WFPROCESS_WRAPPER_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processWrapper");
    public static final QName WFLASTVARIABLES_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "lastVariables");
    public static final QName WFLAST_DETAILS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "lastDetails");
    public static final QName WFSTATUS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "status");
}
