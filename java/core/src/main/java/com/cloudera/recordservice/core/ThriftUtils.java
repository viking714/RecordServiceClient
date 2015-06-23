// Copyright 2014 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.recordservice.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.sasl.Sasl;

import org.apache.thrift.transport.TSaslClientTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.recordservice.thrift.TProtocolVersion;

/**
 * Utility class to convert from thrift classes to client classes. We should
 * never be returning thrift classes as part of the client API.
 */
public class ThriftUtils {
  private final static Logger LOG = LoggerFactory.getLogger(ThriftUtils.class);
  static {
    // This is required to allow clients to connect via kerberos. This is called
    // when the kerberos connection is being opened. The option we need is to
    // use the ticketCache.
    // TODO: is this the best way to do this?
    Configuration.setConfiguration(new Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        final Map<String, String> options = new HashMap<String, String>();
        options.put("useTicketCache", "true");
        return new AppConfigurationEntry[]{
                new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)};
      }
    });
  }

  /**
   * Connects to a thrift service running at hostname/port, returning a TTransport
   * object to that service. If kerberosPrincipal is not null, the connection will
   * be kerberized.
   * TODO: this should also support delegation tokens.
   */
  public static TTransport createTransport(String service, String hostname, int port,
      String kerberosPrincipal) throws IOException {
    if (kerberosPrincipal == null) {
      LOG.info(String.format("Connecting to %s at %s:%d", service, hostname, port));
    } else {
      LOG.info(String.format("Connecting to %s at %s:%d with kerberos principal:%s",
          service, hostname, port, kerberosPrincipal));
    }
    TTransport transport = new TSocket(hostname, port);

    if (kerberosPrincipal != null) {
      // Kerberized, wrap the transport in a sasl transport.
      String[] names = kerberosPrincipal.split("[/@]");
      if (names.length != 3) {
        throw new IllegalArgumentException("Kerberos principal should have 3 parts: "
            + kerberosPrincipal);
      }
      System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
      Map<String, String> saslProps = new HashMap<String, String>();
      saslProps.put(Sasl.SERVER_AUTH, "true");
      transport = new TSaslClientTransport("GSSAPI", null,
          names[0], names[1], saslProps, null, transport);
    }

    try {
      transport.open();
    } catch (TTransportException e) {
      String msg = String.format(
          "Could not connect to %s: %s:%d", service, hostname, port);
      LOG.warn(String.format("%s: error: %s", msg, e));
      throw new IOException(msg, e);
    }

    LOG.info(String.format("Connected to %s at %s:%d", service, hostname, port));
    return transport;
  }

  protected static ProtocolVersion fromThrift(TProtocolVersion v) {
    switch (v) {
    case V1: return ProtocolVersion.V1;
    default:
      // TODO: is this right for mismatched versions? Default to a lower
      // version?
      throw new RuntimeException("Unrecognized version: " + v);
    }
  }
}
