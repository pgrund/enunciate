package org.codehaus.enunciate.contract.jaxrs;

import javax.ws.rs.NotFoundException;
import junit.framework.TestCase;

/**
 * @author Ryan Heaton
 */
public class TestResourceMethod extends TestCase {

  public void testScrubPaths() throws Exception {
    assertEquals("/projects/p/{projectSlug}", ResourceMethod.scrubParamNames("/projects/p/{projectSlug:[a-zA-Z0-9]+([a-zA-Z0-9_\\-{.}]*[a-zA-Z0-9]+)?}"));
  }
  
  public void testJaxRs() throws Exception {
      NotFoundException nf = new NotFoundException();
      System.out.println(nf.getResponse().getStatus());
  }
}
