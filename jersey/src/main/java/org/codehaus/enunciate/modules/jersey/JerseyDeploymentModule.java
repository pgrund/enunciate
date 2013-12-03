/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.modules.jersey;

import com.sun.mirror.type.ReferenceType;
import freemarker.template.TemplateException;
import org.apache.commons.digester.RuleSet;
import org.codehaus.enunciate.EnunciateException;
import org.codehaus.enunciate.apt.EnunciateClasspathListener;
import org.codehaus.enunciate.apt.EnunciateFreemarkerModel;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethod;
import org.codehaus.enunciate.contract.jaxrs.RootResource;
import org.codehaus.enunciate.contract.validation.ValidationException;
import org.codehaus.enunciate.contract.validation.Validator;
import org.codehaus.enunciate.main.Enunciate;
import org.codehaus.enunciate.main.webapp.BaseWebAppFragment;
import org.codehaus.enunciate.main.webapp.WebAppComponent;
import org.codehaus.enunciate.modules.FreemarkerDeploymentModule;
import org.codehaus.enunciate.modules.SpecProviderModule;
import org.codehaus.enunciate.modules.jersey.config.JerseyRuleSet;
import org.codehaus.enunciate.template.freemarker.ClassForNameMethod;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.ws.rs.WebApplicationException;


/**
 * <h1>Jersey Module</h1>
 *
 * <p>The Jersey module generates and compiles the support files and classes necessary to support a REST application according to
 * <a href="https://jsr311.dev.java.net/">JSR-311</a>, using <a href="https://jersey.dev.java.net/">Jersey</a>.</p>
 *
 * <ul>
 * <li><a href="#app">Jersey Application</a></li>
 * <li><a href="#steps">steps</a></li>
 * <li><a href="#config">configuration</a></li>
 * <li><a href="#artifacts">artifacts</a></li>
 * </ul>
 *
 * <h1><a name="app">Jersey Application</a></h1>
 *
 * <p>We direct you do the documentation for <a href="https://jsr311.dev.java.net/">JAX-RS</a> and <a href="https://jersey.dev.java.net/">Jersey</a> to
 * learn how to build a REST application using these technologies. However, it is important to note a few idiosyncrasies of the Enunciate-supported
 * Jersey application.</p>
 *
 * <h3>REST subcontext</h3>
 *
 * <p>Because the Jersey application is presumably deployed along with other Enunciate-supported applications (JAX-WS for SOAP, API documentation, etc.),
 * it will, by default, be mounted at a specific subcontext as defined in the Enunciate configuration (attribute "defaultRestSubcontext" of the
 * "enunciate/services/rest" element). This means that a JAX-RS resource applied at path "mypath" will actually be mounted at "rest/mypath", assuming
 * that "rest" is the subcontext (which it is by default).</p>
 *
 * <p>While is it recommended that the subcontext be preserved, you can disable it in the <a href="#config">configuration</a> for this module. Note, however,
 * that this increases the chance of the paths of your REST resources conflicting with the paths of your documentation, SOAP endpoints, etc.  Enunciate
 * provides an additional check to see if a REST resource is too greedy because it has a <a href="https://jsr311.dev.java.net/nonav/javadoc/javax/ws/rs/Path.html">path
 * parameter</a> in the first path segment.  This can also be disabled in configuration, but doing so will effectively disable the Enunciate-generated
 * documentation and other web service endpoints.</p>
 *
 * <h3>Content Negotiation</h3>
 *
 * <p>Enuncite provides a special content negotiation (conneg) to Jersey such that that each resource is mounted from the REST subcontext (see above) but
 * ALSO from a subcontext that conforms to the id of each content type that the resource supports.  So, if the content type id of the "application/xml"
 * content type is "xml" then the resource at path "mypath" will be mounted at both "/rest/mypath" and "/xml/mypath". You can disable this path-based content
 * negotiation feature by setting <tt>usePathBasedConneg="false"</tt>.</p>
 *
 * <p>The content types for each JAX-RS resource are declared by the @Produces annotation. The content type ids are customized with the
 * "enunciate/services/rest/content-types" element in the Enunciate configuration. Enunciate supplies providers for the "application/xml" and "application/json"
 * content types by default.</p>
 *
 * <h1><a name="steps">Steps</a></h1>
 *
 * <h3>generate</h3>
 *
 * <p>The generate step of the Jersey module generates the configuration files for a servlet-based Jersey application.</p>
 *
 * <h1><a name="config">Configuration</a></h1>
 *
 * <p>The Jersey module supports the following attributes:</p>
 *
 * <ul>
 * <li>The "useSubcontext" attribute is used to enable/disable mounting the JAX-RS resources at the rest subcontext. Default: "true".</li>
 * <li>The "usePathBasedConneg" attribute is used to enable/disable path-based conneg (see above). Default: "true".</a></li>
 * <li>The "useWildcardServletMapping" attribute is used to tell Enunciate to use a wildcard to map to the jersey servlet. By default, Enunciate
 * attempts to map each endpoint to a specific servlet mapping. Default: "false".</a></li>
 * <li>The "disableWildcardServletError" attribute is used to enable/disable the Enunciate "wildcard" resource check. Default: "false".</a></li>
 * <li>The "resourceProviderFactory" attribute is used to specify the fully-qualified classname of an instance of
 * com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory that jersey will use. The default is the spring-based factory or the
 * jersey default instance if spring isn't enabled.</a></li>
 * <li>The "defaultNamespace" attribute is used to specify the default XML namespace. This namespace will have no prefix during XML serialization.</li>
 * <li>The "loadOnStartup" attribute is used to specify the order in which the servlet is loaded on startup by the web application. By default, no order is specified.</li>
 * </ul>
 *
 * <p>The Jersey module also supports an arbitrary number of "init-param" child elements that can be used to specify the init parameters (e.g.
 * container request filters, etc.) of the Jersey servlet. The "init-param" element supports a "name" attribute and a "value" attribute.</p>
 *
 * <h1><a name="artifacts">Artifacts</a></h1>
 *
 * <p>The Jersey deployment module exports no artifacts.</p>
 *
 * @author Ryan Heaton
 * @docFileName module_jersey.html
 */
public class JerseyDeploymentModule extends FreemarkerDeploymentModule implements EnunciateClasspathListener, SpecProviderModule {

  private boolean jacksonAvailable = false;
  private boolean useSubcontext = true;
  private boolean usePathBasedConneg = true;
  private boolean disableWildcardServletError = false;
  private boolean useWildcardServletMapping = false;
  private String resourceProviderFactory = null;
  private String applicationClass = null;
  private String defaultNamespace = null;
  private String loadOnStartup = null;
  private final Map<String, String> servletInitParams = new HashMap<String, String>();

  /**
   * @return "jersey"
   */
  @Override
  public String getName() {
    return "jersey";
  }

  /**
   * The root resources template URL.
   *
   * @return The root resources template URL.
   */
  public URL getRootResourceListTemplateURL() {
    return JerseyDeploymentModule.class.getResource("jaxrs-root-resources.list.fmt");
  }

  /**
   * The providers template URL.
   *
   * @return The providers template URL.
   */
  public URL getProvidersListTemplateURL() {
    return JerseyDeploymentModule.class.getResource("jaxrs-providers.list.fmt");
  }

  /**
   * The jaxb types template URL.
   *
   * @return The jaxb types template URL.
   */
  public URL getJaxbTypesTemplateURL() {
    return JerseyDeploymentModule.class.getResource("jaxrs-jaxb-types.list.fmt");
  }

  /**
   * @return A new {@link JerseyValidator}.
   */
  @Override
  public Validator getValidator() {
    return new JerseyValidator(isUseSubcontext() || !isDisableWildcardServletError());
  }

  @Override
  public void init(Enunciate enunciate) throws EnunciateException {
    super.init(enunciate);

    if (!isDisabled()) {
      enunciate.getConfig().addCustomResourceParameterAnnotation("com.sun.jersey.multipart.FormDataParam"); //support for multipart parameters
      enunciate.getConfig().addSystemResourceParameterAnnotation("com.sun.jersey.api.core.InjectParam"); //support for inject param.
    }
  }

  // Inherited.
  @Override
  public void initModel(EnunciateFreemarkerModel model) {
    super.initModel(model);

    if (!isDisabled()) {
      Map<String, String> contentTypes2Ids = model.getContentTypesToIds();

      if (getEnunciate().isModuleEnabled("amf")) { //if the amf module is enabled, we'll add amf rest endpoints.
        contentTypes2Ids.put("application/x-amf", "amf");
      }
      else {
        debug("AMF module has been disabled, so it's assumed the REST endpoints won't be available in AMF format.");
      }

      if (jacksonAvailable) {
        contentTypes2Ids.put("application/json", "json"); //if we can load jackson, we've got json.
      }
      else {
        debug("Couldn't find Jackson on the classpath, so it's assumed the REST endpoints aren't available in JSON format.");
      }

      for (RootResource resource : model.getRootResources()) {
        for (ResourceMethod resourceMethod : resource.getResourceMethods(true)) {
          Map<String, Set<String>> subcontextsByContentType = new HashMap<String, Set<String>>();
          String subcontext = isUseSubcontext() ? getRestSubcontext() : "";
          debug("Resource method %s of resource %s to be made accessible at subcontext \"%s\".",
                resourceMethod.getSimpleName(), resourceMethod.getParent().getQualifiedName(), subcontext);
          subcontextsByContentType.put(null, new TreeSet<String>(Arrays.asList(subcontext)));
          resourceMethod.putMetaData("defaultSubcontext", subcontext);

          if (isUsePathBasedConneg()) {
            for (String producesMime : resourceMethod.getProducesMime()) {
              MediaType producesType = MediaType.valueOf(producesMime);

              for (Map.Entry<String, String> contentTypeToId : contentTypes2Ids.entrySet()) {
                MediaType type = MediaType.valueOf(contentTypeToId.getKey());
                if (producesType.isCompatible(type)) {
                  String id = '/' + contentTypeToId.getValue();
                  String fullpath = resourceMethod.getFullpath();
                  if (fullpath.startsWith(id) || fullpath.startsWith(contentTypeToId.getValue())) {
                    throw new ValidationException(resourceMethod.getPosition(), String.format("The path of this resource starts with \"%s\" and you've got path-based conneg enabled. So Enunciate can't tell whether a request for \"%s\" is a request for this resource or a request for the \"%s\" representation of resource \"%s\". You're going to have to either adjust the path of the resource or disable path-based conneg in the enunciate config (e.g. usePathBasedConneg=\"false\").", id, fullpath, id, fullpath.substring(fullpath.indexOf(contentTypeToId.getValue()) + contentTypeToId.getValue().length())));
                  }

                  debug("Resource method %s of resource %s to be made accessible at subcontext \"%s\" because it produces %s/%s.",
                        resourceMethod.getSimpleName(), resourceMethod.getParent().getQualifiedName(), id, producesType.getType(), producesType.getSubtype());
                  String contentTypeValue = String.format("%s/%s", type.getType(), type.getSubtype());
                  Set<String> subcontextList = subcontextsByContentType.get(contentTypeValue);
                  if (subcontextList == null) {
                    subcontextList = new TreeSet<String>();
                    subcontextsByContentType.put(contentTypeValue, subcontextList);
                  }
                  subcontextList.add(id);
                }
              }
            }
          }

          resourceMethod.putMetaData("subcontexts", subcontextsByContentType);
        }
      }
    }
  }

  // Inherited.
  public void onClassesFound(Set<String> classes) {
    jacksonAvailable |= classes.contains("org.codehaus.jackson.jaxrs.JacksonJsonProvider");
  }

  public void doFreemarkerGenerate() throws EnunciateException, IOException, TemplateException {
    if (!isUpToDate()) {
      EnunciateFreemarkerModel model = getModel();
      model.put("forName", new ClassForNameMethod());
      processTemplate(getRootResourceListTemplateURL(), model);
      processTemplate(getProvidersListTemplateURL(), model);
      processTemplate(getJaxbTypesTemplateURL(), model);

      Map<String, String> conentTypesToIds = model.getContentTypesToIds();
      Properties mappings = new Properties();
      for (Map.Entry<String, String> contentTypeToId : conentTypesToIds.entrySet()) {
        mappings.put(contentTypeToId.getValue(), contentTypeToId.getKey());
      }
      File file = new File(getGenerateDir(), "media-type-mappings.properties");
      FileOutputStream out = new FileOutputStream(file);
      mappings.store(out, "JAX-RS media type mappings.");
      out.flush();
      out.close();

      Map<String, String> ns2prefixes = model.getNamespacesToPrefixes();
      mappings = new Properties();
      for (Map.Entry<String, String> ns2prefix : ns2prefixes.entrySet()) {
        mappings.put(ns2prefix.getKey() == null ? "" : ns2prefix.getKey(), ns2prefix.getValue());
      }
      if (this.defaultNamespace != null) {
        mappings.put("--DEFAULT_NAMESPACE_ALIAS--", this.defaultNamespace);
      }
      file = new File(getGenerateDir(), "ns2prefix.properties");
      out = new FileOutputStream(file);
      mappings.store(out, "Namespace to prefix mappings.");
      out.flush();
      out.close();
    }
    else {
      info("Skipping generation of JAX-RS support files because everything appears up-to-date.");
    }
  }

  @Override
  protected void doBuild() throws EnunciateException, IOException {
    super.doBuild();

    File webappDir = getBuildDir();
    webappDir.mkdirs();
    File webinf = new File(webappDir, "WEB-INF");
    File webinfClasses = new File(webinf, "classes");
    getEnunciate().copyFile(new File(getGenerateDir(), "jaxrs-providers.list"), new File(webinfClasses, "jaxrs-providers.list"));
    getEnunciate().copyFile(new File(getGenerateDir(), "jaxrs-root-resources.list"), new File(webinfClasses, "jaxrs-root-resources.list"));
    getEnunciate().copyFile(new File(getGenerateDir(), "jaxrs-jaxb-types.list"), new File(webinfClasses, "jaxrs-jaxb-types.list"));
    getEnunciate().copyFile(new File(getGenerateDir(), "media-type-mappings.properties"), new File(webinfClasses, "media-type-mappings.properties"));
    getEnunciate().copyFile(new File(getGenerateDir(), "ns2prefix.properties"), new File(webinfClasses, "ns2prefix.properties"));

    BaseWebAppFragment webappFragment = new BaseWebAppFragment(getName());
    webappFragment.setBaseDir(webappDir);
    WebAppComponent servletComponent = new WebAppComponent();
    servletComponent.setName("jersey");
    servletComponent.setClassname(EnunciateJerseyServletContainer.class.getName());
    TreeMap<String, String> initParams = new TreeMap<String, String>();
    initParams.putAll(getServletInitParams());
    if (!isUsePathBasedConneg()) {
      initParams.put(JerseyAdaptedHttpServletRequest.FEATURE_PATH_BASED_CONNEG, Boolean.FALSE.toString());
    }
    if (isUseSubcontext()) {
      initParams.put(JerseyAdaptedHttpServletRequest.PROPERTY_SERVLET_PATH, getRestSubcontext());
    }
    if (getResourceProviderFactory() != null) {
      initParams.put(JerseyAdaptedHttpServletRequest.PROPERTY_RESOURCE_PROVIDER_FACTORY, getResourceProviderFactory());
    }
    if (getApplicationClass() != null) {
      initParams.put("javax.ws.rs.Application", getApplicationClass());
    }
    if (getLoadOnStartup() != null) {
      servletComponent.setLoadOnStartup(getLoadOnStartup());
    }
    servletComponent.setInitParams(initParams);

    TreeSet<String> urlMappings = new TreeSet<String>();
    for (RootResource rootResource : getModel().getRootResources()) {
      for (ResourceMethod resourceMethod : rootResource.getResourceMethods(true)) {
        String resourceMethodPattern = resourceMethod.getServletPattern();
        for (Set<String> subcontextList : ((Map<String, Set<String>>) resourceMethod.getMetaData().get("subcontexts")).values()) {
          for (String subcontext : subcontextList) {
            String servletPattern;
            if ("".equals(subcontext)) {
              servletPattern = isUseWildcardServletMapping() ? "/*" : resourceMethodPattern;
            }
            else {
              servletPattern = isUseWildcardServletMapping() ? subcontext + "/*" : subcontext + resourceMethodPattern;
            }

            if (urlMappings.add(servletPattern)) {
              debug("Resource method %s of resource %s to be made accessible by servlet pattern %s.",
                    resourceMethod.getSimpleName(), resourceMethod.getParent().getQualifiedName(), servletPattern);
            }
          }
        }
      }
    }

    if (urlMappings.contains("/*")) {
      urlMappings.clear();
      urlMappings.add("/*");
    }
    else {
      Iterator<String> iterator = urlMappings.iterator();
      while (iterator.hasNext()) {
        String mapping = iterator.next();
        if (!mapping.endsWith("/*") && urlMappings.contains(mapping + "/*")) {
          iterator.remove();
        }
      }
    }

    servletComponent.setUrlMappings(urlMappings);
    webappFragment.setServlets(Arrays.asList(servletComponent));
    getEnunciate().addWebAppFragment(webappFragment);
  }

  protected String getRestSubcontext() {
    String restSubcontext = getEnunciate().getConfig().getDefaultRestSubcontext();
    //todo: override default rest subcontext?
    return restSubcontext;
  }

  @Override
  public RuleSet getConfigurationRules() {
    return new JerseyRuleSet();
  }

  /**
   * Whether the generated sources are up-to-date.
   *
   * @return Whether the generated sources are up-to-date.
   */
  protected boolean isUpToDate() {
    return enunciate.isUpToDateWithSources(getGenerateDir());
  }

  // Inherited.
  public boolean isJaxwsProvider() {
    return false;
  }

  // Inherited.
  public boolean isJaxrsProvider() {
    return true;
  }

  /**
   * Whether to use the REST subcontext.
   *
   * @return Whether to use the REST subcontext.
   */
  public boolean isUseSubcontext() {
    return useSubcontext;
  }

  /**
   * Whether to use the REST subcontext.
   *
   * @param useSubcontext Whether to use the REST subcontext.
   */
  public void setUseSubcontext(boolean useSubcontext) {
    this.useSubcontext = useSubcontext;
  }

  /**
   * Whether to use path-based conneg.
   *
   * @return Whether to use path-based conneg.
   */
  public boolean isUsePathBasedConneg() {
    return usePathBasedConneg;
  }

  /**
   * Whether to use path-based conneg.
   *
   * @param usePathBasedConneg Whether to use path-based conneg.
   */
  public void setUsePathBasedConneg(boolean usePathBasedConneg) {
    this.usePathBasedConneg = usePathBasedConneg;
  }

  /**
   * The fully-qualified classname of an instance of com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory that jersey will use.
   *
   * @return The fully-qualified classname of an instance of com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory that jersey will use.
   */
  public String getResourceProviderFactory() {
    return resourceProviderFactory;
  }

  /**
   * The fully-qualified classname of an instance of com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory that jersey will use.
   *
   * @param resourceProviderFactory The fully-qualified classname of an instance of com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory that jersey will use.
   */
  public void setResourceProviderFactory(String resourceProviderFactory) {
    this.resourceProviderFactory = resourceProviderFactory;
  }

  /**
   * The fully-qualified classname of an instance of the implementation of javax.ws.rs.core.Application that jersey will use.
   *
   * @return The fully-qualified classname of an instance of the implementation of javax.ws.rs.core.Application that jersey will use.
   */
  public String getApplicationClass() {
    return applicationClass;
  }

  /**
   * The fully-qualified classname of an instance of the implementation of javax.ws.rs.core.Application that jersey will use.'
   *
   * @param applicationClass The fully-qualified classname of an instance of the implementation of javax.ws.rs.core.Application that jersey will use.
   */
  public void setApplicationClass(String applicationClass) {
    this.applicationClass = applicationClass;
  }

  /**
   * The order in which the servlet is loaded on startup by the web application.
   *
   * @return The order in which the servlet is loaded on startup by the web application.
   */
  public String getLoadOnStartup() {
    return loadOnStartup;
  }

  /**
   * The order in which the servlet is loaded on startup by the web application.
   *
   * @param loadOnStartup The order in which the servlet is loaded on startup by the web application.
   */
  public void setLoadOnStartup(String loadOnStartup) {
    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Whether to disable the greedy servlet pattern error.
   *
   * @return Whether to disable the greedy servlet pattern error.
   */
  public boolean isDisableWildcardServletError() {
    return disableWildcardServletError;
  }

  /**
   * Whether to disable the wildcard servlet pattern error.
   *
   * @param disableWildcardServletError Whether to disable the wildcard servlet pattern error.
   */
  public void setDisableWildcardServletError(boolean disableWildcardServletError) {
    this.disableWildcardServletError = disableWildcardServletError;
  }

  /**
   * Whether to use the wildcard servlet mapping.
   *
   * @return Whether to use the wildcard servlet mapping.
   */
  public boolean isUseWildcardServletMapping() {
    return useWildcardServletMapping;
  }

  /**
   * Whether to use the wildcard servlet mapping.
   *
   * @param useWildcardServletMapping Whether to use the wildcard servlet mapping.
   */
  public void setUseWildcardServletMapping(boolean useWildcardServletMapping) {
    this.useWildcardServletMapping = useWildcardServletMapping;
  }

  /**
   * The default namespace. This namespace will have no prefix associated with it during XML serialization.
   *
   * @return The default namespace.
   */
  public String getDefaultNamespace() {
    return defaultNamespace;
  }

  /**
   * The default namespace.
   *
   * @param defaultNamespace The default namespace.
   */
  public void setDefaultNamespace(String defaultNamespace) {
    this.defaultNamespace = defaultNamespace;
  }

  /**
   * Get the servlet init params.
   *
   * @return The servlet init params.
   */
  public Map<String, String> getServletInitParams() {
    return servletInitParams;
  }

  /**
   * Add a servlet init param.
   *
   * @param name The name of the init param.
   * @param value The value of the init param.
   */
  public void addServletInitParam(String name, String value) {
    this.servletInitParams.put(name, value);
  }

  // Inherited.
  @Override
  public boolean isDisabled() {
    if (super.isDisabled()) {
      return true;
    }
    else if (getModelInternal() != null && getModelInternal().getRootResources().isEmpty()) {
      debug("Jersey module is disabled because there are no root resources.");
      return true;
    }
    else if (getModelInternal() != null && getModelInternal().getEnunciateConfig() != null && getModelInternal().getEnunciateConfig().getWebAppConfig() != null && getModelInternal().getEnunciateConfig().getWebAppConfig().isDisabled()) {
      debug("Module '%s' is disabled because the web application processing has been disabled.", getName());
      return true;
    }

    return false;
  }
}