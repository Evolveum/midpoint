/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.MidPointApplicationConfiguration;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.error.*;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.MidPointResourceStreamLocator;
import com.evolveum.midpoint.web.util.MidPointStringResourceLoader;
import com.evolveum.midpoint.web.util.SchrodingerComponentInitListener;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.core.util.resource.locator.caching.CachingResourceStreamLocator;
import org.apache.wicket.markup.head.PriorityFirstComparator;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.ApplicationSettings;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.lang.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.web.csrf.CsrfToken;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author lazyman
 */
public class MidPointApplication extends AuthenticatedWebApplication {

    public static final String SYSTEM_PROPERTY_SCHRODINGER = "midpoint.schrodinger";

    /**
     * Max. photo size for user/jpegPhoto
     */
    public static final Bytes FOCUS_PHOTO_MAX_FILE_SIZE = Bytes.kilobytes(192);

    public static final String WEB_APP_CONFIGURATION = "midpoint.webApplication";

    public static final List<LocaleDescriptor> AVAILABLE_LOCALES;

    private static final String LOCALIZATION_DESCRIPTOR = "localization/locale.properties";

    private static final String PROP_NAME = ".name";
    private static final String PROP_FLAG = ".flag";
    private static final String PROP_DEFAULT = ".default";

    private static final Trace LOGGER = TraceManager.getTrace(MidPointApplication.class);

    static {
        SchemaDebugUtil.initialize();
    }

    static {
        String midpointHome = System.getProperty(WebApplicationConfiguration.MIDPOINT_HOME);
        File file = new File(midpointHome, LOCALIZATION_DESCRIPTOR);

        Resource[] localeDescriptorResources = new Resource[]{
                new FileSystemResource(file),
                new ClassPathResource(LOCALIZATION_DESCRIPTOR)
        };

        List<LocaleDescriptor> locales = new ArrayList<>();
        for (Resource resource : localeDescriptorResources) {
            if (!resource.isReadable()) {
                continue;
            }

            try {
                LOGGER.debug("Found localization descriptor {}.", new Object[]{resource.getURL()});
                locales = loadLocaleDescriptors(resource);

                break;
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load localization", ex);
            }
        }

        Collections.sort(locales);

        AVAILABLE_LOCALES = Collections.unmodifiableList(locales);
    }

    @Autowired
    transient ModelService model;
    @Autowired
    transient ModelInteractionService modelInteractionService;
    @Autowired
    transient TaskService taskService;
    @Autowired
    transient PrismContext prismContext;
    @Autowired
    transient ExpressionFactory expressionFactory;
    @Autowired
    transient TaskManager taskManager;
    @Autowired
    transient ModelAuditService auditService;
    @Autowired
    transient private RepositoryService repositoryService;            // temporary
    @Autowired
    transient private WorkflowService workflowService;
    @Autowired
    transient private WorkflowManager workflowManager;
    @Autowired
    transient MidpointConfiguration configuration;
    @Autowired
    transient Protector protector;
    @Autowired
    transient MatchingRuleRegistry matchingRuleRegistry;
    @Autowired
    transient SecurityEnforcer securityEnforcer;
    @Autowired
    transient SecurityContextManager securityContextManager;
    @Autowired
    transient SystemObjectCache systemObjectCache;
    @Autowired
    transient LocalizationService localizationService;
    @Autowired
    transient AsyncWebProcessManager asyncWebProcessManager;
    @Autowired
    transient ApplicationContext applicationContext;

    private WebApplicationConfiguration webApplicationConfiguration;

    @Override
    public Class<? extends PageBase> getHomePage() {
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL)) {
            return PageDashboard.class;
        } else {
            return PageSelfDashboard.class;
        }
    }

    @Override
    public void init() {
        super.init();

        getJavaScriptLibrarySettings().setJQueryReference(
                new PackageResourceReference(MidPointApplication.class,
                        "../../../../../webjars/adminlte/2.3.11/plugins/jQuery/jquery-2.2.3.min.js"));

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        ResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setParentFolderPlaceholder("$-$");
        resourceSettings.setHeaderItemComparator(new PriorityFirstComparator(true));
        SecurePackageResourceGuard guard = (SecurePackageResourceGuard) resourceSettings.getPackageResourceGuard();
        guard.addPattern("+*.woff2");

        List<IStringResourceLoader> resourceLoaders = resourceSettings.getStringResourceLoaders();
        resourceLoaders.add(0, new MidPointStringResourceLoader(localizationService));

        IResourceStreamLocator locator = new CachingResourceStreamLocator(
                new MidPointResourceStreamLocator(resourceSettings.getResourceFinders()));
        resourceSettings.setResourceStreamLocator(locator);

        resourceSettings.setThrowExceptionOnMissingResource(false);
        getMarkupSettings().setStripWicketTags(true);

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getDebugSettings().setAjaxDebugModeEnabled(true);
            getDebugSettings().setDevelopmentUtilitiesEnabled(true);
            initializeDevelopmentSerializers();
        }

        //pretty url for resources (e.g. images)
        mountFiles(ImgResources.BASE_PATH, ImgResources.class);

        //exception handling an error pages
        ApplicationSettings appSettings = getApplicationSettings();
        appSettings.setAccessDeniedPage(PageError401.class);
        appSettings.setInternalErrorPage(PageError.class);
        appSettings.setPageExpiredErrorPage(PageError.class);

        mount(new MountedMapper("/error", PageError.class, new PageParametersEncoder()));
        mount(new MountedMapper("/error/401", PageError401.class, new PageParametersEncoder()));
        mount(new MountedMapper("/error/403", PageError403.class, new PageParametersEncoder()));
        mount(new MountedMapper("/error/404", PageError404.class, new PageParametersEncoder()));
        mount(new MountedMapper("/error/410", PageError410.class, new PageParametersEncoder()));

        getRequestCycleListeners().add(new LoggingRequestCycleListener(this));

        getAjaxRequestTargetListeners().add(new AjaxRequestTarget.AbstractListener() {

            @Override
            public void updateAjaxAttributes(AbstractDefaultAjaxBehavior behavior, AjaxRequestAttributes attributes) {
                // check whether behavior will use POST method, if not then don't put CSRF token there
                if (!isPostMethodTypeBehavior(behavior, attributes)) {
                    return;
                }

                CsrfToken csrfToken = SecurityUtils.getCsrfToken();
                if (csrfToken == null) {
                    return;
                }

                String parameterName = csrfToken.getParameterName();
                String value = csrfToken.getToken();

                attributes.getExtraParameters().put(parameterName, value);
            }
        });

        getSessionListeners().add((ISessionListener) asyncWebProcessManager);

        //descriptor loader, used for customization
        new DescriptorLoader().loadData(this);
        
        if (applicationContext != null) {

	        Map<String, MidPointApplicationConfiguration> map =
	                applicationContext.getBeansOfType(MidPointApplicationConfiguration.class);
	        if (map != null) {
	            map.forEach((key, value) -> value.init(this));
	        }
        }

        // for schrodinger selenide library
        initializeSchrodinger();
    }

    private void initializeSchrodinger() {
    	if (applicationContext == null) {
    		return;
    	}
        Environment environment = applicationContext.getEnvironment();
        if (environment == null) {
        	return;
        }

        String value = environment.getProperty(SYSTEM_PROPERTY_SCHRODINGER);
        Boolean enabled = Boolean.parseBoolean(value);

        if (enabled) {
            LOGGER.info("Schrodinger plugin enabled");
            getComponentInitializationListeners().add(new SchrodingerComponentInitListener());
        }
    }

    private boolean isPostMethodTypeBehavior(AbstractDefaultAjaxBehavior behavior, AjaxRequestAttributes attributes) {
        if (behavior instanceof AjaxFormComponentUpdatingBehavior) {
            // these also uses POST, but they set it after this method is called
            return true;
        }

        if (behavior instanceof AjaxFormSubmitBehavior) {
            AjaxFormSubmitBehavior fb = (AjaxFormSubmitBehavior) behavior;
            Form form = fb.getForm();
            String formMethod = form.getMarkupAttributes().getString("method");
            if (formMethod == null || "POST".equalsIgnoreCase(formMethod) || form.getRootForm().isMultiPart()) {
                // this will also use POST
                return true;
            }
        }

        return AjaxRequestAttributes.Method.POST.equals(attributes.getMethod());
    }

    private static List<LocaleDescriptor> loadLocaleDescriptors(Resource resource) throws IOException {
        List<LocaleDescriptor> locales = new ArrayList<>();

        Properties properties = new Properties();
        Reader reader = null;
        try {
            reader = new InputStreamReader(resource.getInputStream(), "utf-8");
            properties.load(reader);

            Map<String, Map<String, String>> localeMap = new HashMap<>();
            Set<String> keys = (Set) properties.keySet();
            for (String key : keys) {
                String[] array = key.split("\\.");
                if (array.length != 2) {
                    continue;
                }

                String locale = array[0];
                Map<String, String> map = localeMap.get(locale);
                if (map == null) {
                    map = new HashMap<>();
                    localeMap.put(locale, map);
                }

                map.put(key, properties.getProperty(key));
            }

            for (String key : localeMap.keySet()) {
                Map<String, String> localeDefinition = localeMap.get(key);
                if (!localeDefinition.containsKey(key + PROP_NAME)
                        || !localeDefinition.containsKey(key + PROP_FLAG)) {
                    continue;
                }

                LocaleDescriptor descriptor = new LocaleDescriptor(
                        localeDefinition.get(key + PROP_NAME),
                        localeDefinition.get(key + PROP_FLAG),
                        localeDefinition.get(key + PROP_DEFAULT),
                        WebComponentUtil.getLocaleFromString(key)
                );
                locales.add(descriptor);
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return locales;
    }

    private URL buildMidpointHomeLocalizationFolderUrl() {
        String midpointHome = System.getProperty(WebApplicationConfiguration.MIDPOINT_HOME);

        File file = new File(midpointHome, "localization");
        try {
            return file.toURI().toURL();
        } catch (IOException ex) {
            throw new SystemException("Couldn't transform localization folder file to url", ex);
        }
    }

    private void initializeDevelopmentSerializers() {
//    	JavaSerializer javaSerializer = new JavaSerializer( getApplicationKey() ) {
//    	    @Override
//    	    protected ObjectOutputStream newObjectOutputStream(OutputStream out) throws IOException {
//    	    	LOGGER.info("XXXXXXX YX Y");
////    	    	IObjectChecker checker1 = new MidPointObjectChecker();
//////    	        IObjectChecker checker2 = new NotDetachedModelChecker();
////    	        IObjectChecker checker3 = new ObjectSerializationChecker();
////    	        return new CheckingObjectOutputStream(out, checker1, checker3);
//    	        return new ObjectOutputStream(out);
//    	    }
//    	};
//    	getFrameworkSettings().setSerializer( javaSerializer );

    }

    private void mountFiles(String path, Class<?> clazz) {
        try {
            List<Resource> list = new ArrayList<>();
            String packagePath = clazz.getPackage().getName().replace('.', '/');

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] res = resolver.getResources("classpath:" + packagePath + "/*.png");
            if (res != null) {
                list.addAll(Arrays.asList(res));
            }
            res = resolver.getResources("classpath:" + packagePath + "/*.gif");
            if (res != null) {
                list.addAll(Arrays.asList(res));
            }

            for (Resource resource : list) {
                URI uri = resource.getURI();
                File file = new File(uri.toString());
                mountResource(path + "/" + file.getName(), new SharedResourceReference(clazz, file.getName()));
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't mount files", ex);
        }
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        if (webApplicationConfiguration == null) {
            Configuration config = configuration.getConfiguration(WEB_APP_CONFIGURATION);
            webApplicationConfiguration = new WebApplicationConfiguration(config);
        }
        return webApplicationConfiguration;
    }

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }

    public ModelService getModel() {
        return model;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public ModelAuditService getAuditService() {
        return auditService;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public Protector getProtector() {
        return protector;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return PageLogin.class;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return MidPointAuthWebSession.class;
    }

    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    public static boolean containsLocale(Locale locale) {
        if (locale == null) {
            return false;
        }

        for (LocaleDescriptor descriptor : AVAILABLE_LOCALES) {
            if (locale.equals(descriptor.getLocale())) {
                return true;
            }
        }

        return false;
    }

    public static Locale getDefaultLocale() {
        for (LocaleDescriptor descriptor : AVAILABLE_LOCALES) {
            if (descriptor.isDefault()) {
                return descriptor.getLocale();
            }
        }

        return new Locale("en", "US");
    }

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public SystemConfigurationType getSystemConfiguration() throws SchemaException {
        PrismObject<SystemConfigurationType> config = systemObjectCache.getSystemConfiguration(new OperationResult("dummy"));
        return config != null ? config.asObjectable() : null;
    }

    public SystemConfigurationType getSystemConfigurationIfAvailable() {
        try {
            PrismObject<SystemConfigurationType> config = systemObjectCache.getSystemConfiguration(new OperationResult("dummy"));
            return config != null ? config.asObjectable() : null;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve system configuration", e);
            return null;
        }
    }

    public static MidPointApplication get() {
        return (MidPointApplication) WebApplication.get();
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        if (user == null) {
            throw new RestartResponseException(PageLogin.class);
        }
        return WebModelServiceUtils.createSimpleTask(operation, user.getUser().asPrismObject(), getTaskManager());
    }

    public AsyncWebProcessManager getAsyncWebProcessManager() {
        return asyncWebProcessManager;
    }

    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    private static class Utf8BundleStringResourceLoader implements IStringResourceLoader {

        private final String bundleName;

        public Utf8BundleStringResourceLoader(String bundleName) {
            this.bundleName = bundleName;
        }

        @Override
        public String loadStringResource(Class<?> clazz, String key, Locale locale, String style, String variation) {
            return loadStringResource((Component) null, key, locale, style, variation);
        }

        @Override
        public String loadStringResource(Component component, String key, Locale locale, String style, String variation) {
            if (locale == null) {
                locale = Session.exists() ? Session.get().getLocale() : Locale.getDefault();
            }

            ResourceBundle.Control control = new UTF8Control();
            try {
                return ResourceBundle.getBundle(bundleName, locale, control).getString(key);
            } catch (MissingResourceException ex) {
                try {
                    return ResourceBundle.getBundle(bundleName, locale,
                            Thread.currentThread().getContextClassLoader(), control).getString(key);
                } catch (MissingResourceException ex2) {
                    return null;
                }
            }
        }

        private static class UTF8Control extends ResourceBundle.Control {

            @Override
            public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                                            boolean reload) throws IllegalAccessException, InstantiationException,
                    IOException {

                // The below is a copy of the default implementation.
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                ResourceBundle bundle = null;
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    try {
                        // Only this line is changed to make it to read properties files as UTF-8.
                        bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
                return bundle;
            }
        }
    }
}
