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

package org.codehaus.enunciate.modules.jersey.response;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

/**
 * A response that includes a status message.
 *
 * @author Ryan Heaton
 */
public class StatusMessageResponse extends Response implements HasStatusMessage {

  private final String statusMessage;
  private final Response delegate;

  public StatusMessageResponse(Response delegate, String statusMessage) {
    this.delegate = delegate;
    this.statusMessage = statusMessage;
  }

  public Object getEntity() {
    return delegate.getEntity();
  }

  public int getStatus() {
    return delegate.getStatus();
  }

  public MultivaluedMap<String, Object> getMetadata() {
    return delegate.getMetadata();
  }

  /**
   * The status message.
   *
   * @return The status message.
   */
  public String getStatusMessage() {
    return this.statusMessage;
  }
// ADDITIONAL

    @Override
    public StatusType getStatusInfo() {
        return delegate.getStatusInfo();
    }

    @Override
    public <T> T readEntity(Class<T> arg0) {
        return delegate.readEntity(arg0);
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0) {
        return delegate.readEntity(arg0);
    }

    @Override
    public <T> T readEntity(Class<T> arg0, Annotation[] arg1) {
        return delegate.readEntity(arg0, arg1);
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0, Annotation[] arg1) {
        return delegate.readEntity(arg0, arg1);
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public boolean bufferEntity() {
        return delegate.bufferEntity();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public MediaType getMediaType() {
        return delegate.getMediaType();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return delegate.getAllowedMethods();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return delegate.getEntityTag();
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Date getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public URI getLocation() {
        return delegate.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return delegate.getLinks();
    }

    @Override
    public boolean hasLink(String arg0) {
        return delegate.hasLink(arg0);
    }

    @Override
    public Link getLink(String arg0) {
        return delegate.getLink(arg0);
    }

    @Override
    public Link.Builder getLinkBuilder(String arg0) {
        return delegate.getLinkBuilder(arg0);
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return delegate.getStringHeaders();
    }

    @Override
    public String getHeaderString(String arg0) {
        return delegate.getHeaderString(arg0);
    }
  
}
