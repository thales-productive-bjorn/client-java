/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.api.server;

import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;

@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
//This class is meant to block incoming requests that are not authorized, based on the client certificate
public class ArrowheadSecurityFilter implements ContainerRequestFilter {
  protected final Logger log = Logger.getLogger(getClass());

  @Context
  Configuration configuration;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    SecurityContext sc = requestContext.getSecurityContext();
    if (sc.isSecure()) {
      String requestTarget = Utility.stripEndSlash(requestContext.getUriInfo().getRequestUri().toString());
      String subjectName = sc.getUserPrincipal().getName();
      if (isClientAuthorized(subjectName)) {
        log.info("SSL identification is successful! Cert: " + subjectName);
      } else {
        throw new AuthException(SecurityUtils.getCertCNFromSubject(subjectName) + " is unauthorized to access " + requestTarget);
      }
    }
  }

  /*
    NOTE right now, every client has the same access control strategy: only requests from the local Cloud are allowed
    NOTE this method should be modified for customized behaviour
   */
  private boolean isClientAuthorized(String subjectName) {
    String clientCN = SecurityUtils.getCertCNFromSubject(subjectName);
    String serverCN = (String) configuration.getProperty("server_common_name");

    if (!SecurityUtils.isKeyStoreCNArrowheadValid(clientCN)) {
      log.warn("Client cert does not have 5 parts, so the access will be denied.");
      return false;
    }
    // All requests from the local cloud are allowed, so omit the first part of the common names (systemName)
    String[] serverFields = serverCN.split("\\.", 2);
    String[] clientFields = clientCN.split("\\.", 2);
    // serverFields contains: systemName, cloudName.operator.arrowhead.eu

    // If this is true, then the certificates are from the same cloud
    return serverFields[1].equalsIgnoreCase(clientFields[1]);
  }

}
