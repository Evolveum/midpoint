Paste this to System Configuration replacing existing <notificationConfiguration>.

<notificationConfiguration>
    <handler>
    <simpleUserNotifier>
        <recipientExpression>
            <value>iamnotify@example.com</value>
        </recipientExpression>
     <subjectExpression>
       <script><code>
if (event.isSuccess())
	tmpText = "[IDM] SUCCESS: User " + event.getChangeType() + " operation succeeded for "
else if (event.isFailure())
	tmpText = "[IDM] ERROR: User " + event.getChangeType() + " operation failed for "
else  tmpText = "[IDM] IN PROGRESS: User " + event.getChangeType() + " operation in progress for "

tmpText + requestee?.getName()?.getOrig()
</code></script>
     </subjectExpression>
        <transport>mail</transport>
</simpleUserNotifier>
</handler>
<handler>
    <accountPasswordNotifier>
    <name>New accounts in midPoint managed systems</name>
     <operation>add</operation>
     <status>success</status>
        <recipientExpression>
	    <script>
	        <code>requestee?.getEmailAddress()</code>
	    </script>
        </recipientExpression>
        <subjectExpression>
           <script><code>"[IDM] New account created by IDM for user: " + requestee?.getName()?.getOrig()</code></script>
        </subjectExpression>
        <transport>mail</transport>
    </accountPasswordNotifier>
    </handler>
    <handler>
    <userPasswordNotifier>
    <name>New user in midPoint</name>
   <operation>add</operation>
        <recipientExpression>
	    <script>
	        <code>requestee?.getEmailAddress()</code>
	    </script>
        </recipientExpression>
        <subjectExpression>
           <script><code>"[IDM] New user created in IDM: " + requestee?.getName()?.getOrig()</code></script>
        </subjectExpression>
        <transport>mail</transport>
    </userPasswordNotifier>
</handler>
<handler>
   <simpleResourceObjectNotifier>
   <name>Notify system administrator for accounts</name>
     <recipientExpression>
        <value>iamnotify@example.com</value>
     </recipientExpression>
     <subjectExpression>
       <script><code>
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

if (event?.isShadowKind(ShadowKindType.ACCOUNT)) tmpObject = 'account '
else tmpObject = 'resource object '

if (event.isSuccess())
	tmpText = "[IDM] SUCCESS: " + tmpObject + event?.getChangeType() + " operation succeeded"
else if (event.isFailure())
	tmpText = "[IDM] ERROR: " + tmpObject + event?.getChangeType() + " operation failed"
else  tmpText = "[IDM] IN PROGRESS: " + tmpObject + event?.getChangeType() + " operation in progress"

if (tmpObject == 'account ') return tmpText + ' for ' + requestee?.getName()?.getOrig()
else return tmpText
</code></script>
     </subjectExpression>
     <transport>mail</transport>
    </simpleResourceObjectNotifier>
</handler>

<!-- ******************************************* -->
<handler>
   <simpleFocalObjectNotifier>
   <focusType>OrgType</focusType>
   <name>Notify system administrator for organization change</name>
     <recipientExpression>
        <value>iamnotify@example.com</value>
     </recipientExpression>
     <subjectExpression>
       <script><code>
tmpObject = 'Organization (Tenant) '

if (event.isSuccess())
	tmpText = "[IDM] SUCCESS: " + tmpObject + event?.getChangeType() + " operation succeeded"
else if (event.isFailure())
	tmpText = "[IDM] ERROR: " + tmpObject + event?.getChangeType() + " operation failed"
else  tmpText = "[IDM] IN PROGRESS: " + tmpObject + event?.getChangeType() + " operation in progress"

return tmpText
</code></script>
     </subjectExpression>
     <transport>mail</transport>
    </simpleFocalObjectNotifier>
</handler>
<handler>
   <simpleFocalObjectNotifier>
   <focusType>RoleType</focusType>
   <name>Notify system administrator for role change</name>
     <recipientExpression>
        <value>iamnotify@example.com</value>
     </recipientExpression>
     <subjectExpression>
       <script><code>
tmpObject = 'Role '

if (event.isSuccess())
	tmpText = "[IDM] SUCCESS: " + tmpObject + event?.getChangeType() + " operation succeeded"
else if (event.isFailure())
	tmpText = "[IDM] ERROR: " + tmpObject + event?.getChangeType() + " operation failed"
else  tmpText = "[IDM] IN PROGRESS: " + tmpObject + event?.getChangeType() + " operation in progress"

return tmpText
</code></script>
     </subjectExpression>
     <transport>mail</transport>
    </simpleFocalObjectNotifier>
</handler>
<!-- ******************************************* -->
<!-- REDIRECT -->
    <mail>
        <redirectToFile>/usr/local/apache-tomcat-pokusy/logs/idm-mail-notifications.log</redirectToFile>
    </mail>
</notificationConfiguration>
