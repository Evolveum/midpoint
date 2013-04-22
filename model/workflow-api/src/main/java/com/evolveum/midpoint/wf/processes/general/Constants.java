package com.evolveum.midpoint.wf.processes.general;

/**
 * @author mederly
 */
public class Constants {

    /**
     * This is a name of default panel that displays information about wf process instance running this process.
     * TODO this is a dependency on gui module; however, it is not clear how to do this more cleanly
     * TODO instead of class names here should be something more abstract
     */
    //public static final String DEFAULT_PANEL_NAME = "ItemApprovalDetailsDefaultPanel";
    public static final String DEFAULT_PANEL_NAME = "com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalPanel";
}
