import org.identityconnectors.framework.common.objects.ObjectClass

class BaseScript extends Script {

    public static final String GROUP_NAME = "Group"

    public static final ObjectClass GROUP = new ObjectClass(BaseScript.GROUP_NAME)

    public static final String ORGANIZATION_NAME = "Organization"

    public static final ObjectClass ORGANIZATION = new ObjectClass(BaseScript.ORGANIZATION_NAME)

    @Override
    Object run() {
        return null
    }
}