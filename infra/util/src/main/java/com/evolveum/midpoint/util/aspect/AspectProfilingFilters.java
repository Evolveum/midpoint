/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;
import java.util.Map;

/**
 *  TODO - refactoring was done here, therefore notes and other TODOs are not relevant. Work in progress here
 *  TODO - add profiling to Workflow and Notification subsystems
 *
 *  IMPORTANT:
 *  1. RepositoryCache->getVersion is currently not logged on any level
 *  2. for debug purposes, provisioning is on TRACE level for now
 *  3. Web subsystem filter was removed - this functionality is already covered with servlet filter
 *  4. INDENT feature has been deprecated since we don't need tree indenting in monitored files
 *
 *  TODO - add descriptive description
 *  TODO - Think and create simple design for sync cycles and other synchronous events
 *
 *  TODO - prepare methodBased filters to every monitored midPoint interface (we don't need to monitor some methods)
 *
 *  @author shood
 * */
public class AspectProfilingFilters {

    /* ATTRIBUTES - Class */

    private static Trace LOGGER = TraceManager.getTrace(AspectProfilingFilters.class);

    //Subsystems
    public static final String SUBSYSTEM_REPOSITORY = "REPO";
    public static final String SUBSYSTEM_TASKMANAGER = "TASK";
    public static final String SUBSYSTEM_PROVISIONING = "PROV";
    public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "ROCL";
    public static final String SUBSYSTEM_MODEL = "MODE";
    public static final String SUBSYSTEM_UCF = "_UCF";

    private static boolean isRepositoryProfiled = false;
    private static boolean isTaskManagerProfiled = false;
    private static boolean isProvisioningProfiled = false;
    private static boolean isResourceObjectChangeListenerProfiled = false;
    private static boolean isModelProfiled = false;
    private static boolean isUcfProfiled = false;

    //Model subsystem constants
    public static final String DELTA_ADD = "ADD";
    public static final String DELTA_REPLACE = "REPLACE";
    public static final String DELTA_DELETE = "DELETE";

    //Other
    private static String ARROW_RIGHT = "->";

    /* BEHAVIOR - STATIC */
    /*
    *   Here, we will decide, what filter will be applied (based on subsystem) on method entry
    */
    public static void applyGranularityFilterOnStart(ProceedingJoinPoint pjp, String subsystem){

        if(pjp == null)
            return;

        if(isRepositoryProfiled && SUBSYSTEM_REPOSITORY.equals(subsystem)){
            applyRepositoryFilterOnStart(pjp);
        } else if(isModelProfiled && SUBSYSTEM_MODEL.equals(subsystem)){
            applyModelFilterOnStart(pjp);
        } else if(isProvisioningProfiled && SUBSYSTEM_PROVISIONING.equals(subsystem)){
            applyProvisioningFilterOnStar(pjp);
        } else if(isTaskManagerProfiled && SUBSYSTEM_TASKMANAGER.equals(subsystem)){
            applyTaskManagerFilterOnStar(pjp);
        } else if(isUcfProfiled && SUBSYSTEM_UCF.equals(subsystem)){
            applyUcfFilterOnStar(pjp);
        } else if(isResourceObjectChangeListenerProfiled && SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER.equals(subsystem)){
            applyResourceObjectChangeListenerFilterOnStar(pjp);
        }
    }   //applyGranularityFilterOnStart

    /*
    *   Here, we will decide, what filter will be applied (based on subsystem) on method end
    * */
    public static void applyGranularityFilterOnEnd(ProceedingJoinPoint pjp, String subsystem, long startTime){

        if(pjp == null)
            return;

        if(SUBSYSTEM_REPOSITORY.equals(subsystem)){
            applyRepositoryFilterOnEnd(pjp, startTime);
        } else if(SUBSYSTEM_MODEL.equals(subsystem)){
            applyModelFilterOnEnd(pjp, startTime) ;
        } else if (SUBSYSTEM_PROVISIONING.equals(subsystem)){
            applyProvisioningFilterOnEnd(pjp, startTime);
        } else if (SUBSYSTEM_TASKMANAGER.equals(subsystem)){
            applyTaskManagerFilterOnEnd(pjp, startTime);
        } else if (SUBSYSTEM_UCF.equals(subsystem)){
            applyUcfFilterOnEnd(pjp, startTime);
        } else if(SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER.equals(subsystem)){
            applyapplyResourceObjectChangeListenerFilterOnStarFilterOnEnd(pjp, startTime);
        }
    }   //applyGranularityFilterOnEnd

   /*
   *   ResourceObjectChangeListener filter - on method entry
   * */
    private static void applyResourceObjectChangeListenerFilterOnStar(ProceedingJoinPoint pjp){

    }   //applyResourceObjectChangeListenerFilterOnStar

    /*
    *   ResourceObjectChangeListener Filter - on method exit
    * */
    private static void applyapplyResourceObjectChangeListenerFilterOnStarFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyapplyResourceObjectChangeListenerFilterOnStarFilterOnEnd

    /*
    *   UCF filter - on method entry
    * */
    private static void applyUcfFilterOnStar(ProceedingJoinPoint pjp){

    }   //applyUcfFilterOnStar

    /*
    *   UCF Filter - on method exit
    * */
    private static void applyUcfFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyUcfFilterOnEnd

    /*
    *   Task Manager filter - on method entry
    * */
    private static void applyTaskManagerFilterOnStar(ProceedingJoinPoint pjp){

    }   //applyTaskManagerFilterOnStar

    /*
    *   Task Manager Filter - on method exit
    * */
    private static void applyTaskManagerFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyTaskManagerFilterOnEnd

    /*
    *   Provisioning filter - on method entry
    * */
    private static void applyProvisioningFilterOnStar(ProceedingJoinPoint pjp){

    }   //applyProvisioningFilterOnStar

    /*
    *   Provisioning Filter - on method exit
    * */
    private static void applyProvisioningFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyProvisioningFilterOnEnd

    /*
    *   Model Filter - on method entry
    * */
    private static void applyModelFilterOnStart(ProceedingJoinPoint pjp){

    }   //applyModelFilterOnStart

    /*
    *   Model filter - on method exit
    * */
    private static void applyModelFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyModelFilterOnEnd


    /*
    *   Repository Filter - entry
    *
    *   On debug level - we only want to log fine-grained information (RepositoryCache implementation of RepositoryService)
    *   On trace level - information from SqlRepositoryServiceImpl + operation arguments are logged as well
    *
    * */
    private static void applyRepositoryFilterOnStart(ProceedingJoinPoint pjp){

    }   //applyRepositoryFilter

    /*
     *   Repository Filter - exit
     */
    private static void applyRepositoryFilterOnEnd(ProceedingJoinPoint pjp, long startTime){

    }   //applyRepositoryFilterOnEnd


    /*===== SOME HELP METHODS =====*/
    /*
     *  Returns ObejctType with what operation operates
     */
    private static String getOperationType(ProceedingJoinPoint pjp){
        Object[] args = pjp.getArgs();

        if(args.length == 0)
            return "NO_ARGS";

        try{
            String[] splitType = PrettyPrinter.prettyPrint(args[0]).split("\\.");

            if(splitType.length < 1)
                return "null";
            else
                return splitType[splitType.length -1];

        } catch (Throwable t){
            //TODO - do we want to throw exceptions to profiling files?
            LOGGER.error("Internal error formatting a value: {}", args[0], t);
            return "###INTERNAL#ERROR### "+t.getClass().getName()+": "+t.getMessage()+" value="+args[0];
        }
    }   //getOperationType

    /*
     *  Calculates estimated time on method exit
     */
    private static String calculateTime(long startTime){
        StringBuilder sb = new StringBuilder();

        sb.append(" etime: ");
        // Mark end of processing
        long elapsed = System.nanoTime() - startTime;
        sb.append((long) (elapsed / 1000000));
        sb.append('.');
        long mikros = (long) (elapsed / 1000) % 1000;

        if (mikros < 100)
            sb.append('0');
        if (mikros < 10)
            sb.append('0');

        sb.append(mikros);
        sb.append(" ms");

        return sb.toString();
    }   //calculateTime

    /*
     *  Return type of delta
     */
    private static String getDeltaType(ProceedingJoinPoint pjp){
        String param = getOperationType(pjp);

        if(param.contains(DELTA_ADD))
            return DELTA_ADD;
        else if (param.contains(DELTA_DELETE))
            return DELTA_DELETE;
        else if (param.contains(DELTA_REPLACE))
            return DELTA_REPLACE;

        return "";
    }   //getDeltaType

    /**
     * Get joinpoint class name if available
     *
     * @param pjp
     * @return
     */
    private static String getClassName(ProceedingJoinPoint pjp) {
        String className = null;
        if (pjp.getThis() != null) {
            className = pjp.getThis().getClass().getName();
            className = className.replaceFirst("com.evolveum.midpoint", "..");
        }
        return className;
    }   //getClassName

    /*
    *   Configure profiled subsystems
    * */
    public static void subsystemConfiguration(Map<String, Boolean> subsystems){

        isModelProfiled = subsystems.get(SUBSYSTEM_MODEL);
        isProvisioningProfiled = subsystems.get(SUBSYSTEM_PROVISIONING);
        isRepositoryProfiled = subsystems.get(SUBSYSTEM_REPOSITORY);
        isResourceObjectChangeListenerProfiled = subsystems.get(SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
        isTaskManagerProfiled = subsystems.get(SUBSYSTEM_TASKMANAGER);
        isUcfProfiled = subsystems.get(SUBSYSTEM_UCF);

    }   //subsystemConfiguration

}
