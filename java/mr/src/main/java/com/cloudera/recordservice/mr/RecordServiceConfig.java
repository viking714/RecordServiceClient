// Confidential Cloudera Information: Covered by NDA.
// Copyright 2012 Cloudera Inc.
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

package com.cloudera.recordservice.mr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.StringUtils;

import com.cloudera.recordservice.core.NetworkAddress;

/**
 * Config keys and values for the RecordService and utilities to set them.
 */
public class RecordServiceConfig {
  // The query to generate records from.
  public final static String QUERY_NAME_CONF = "recordservice.query";

  // The fully qualified table name to read.
  public final static String TBL_NAME_CONF = "recordservice.table.name";

  // The subset of columns to read.
  public final static String COL_NAMES_CONF = "recordservice.col.names";

  // Host/Port of the planner service. Comma separated list.
  public final static String PLANNER_HOSTPORTS_CONF = "recordservice.planner.hostports";

  public static final String DEFAULT_PLANNER_HOSTPORTS =
    System.getenv("RECORD_SERVICE_PLANNER_HOST") != null ?
        System.getenv("RECORD_SERVICE_PLANNER_HOST") + ":40000" : "localhost:40000";

  // Kerberos principal.
  public final static String KERBEROS_PRINCIPAL_CONF = "recordservice.kerberos.principal";

  // Optional configuration option for performance tuning that configures the max number
  // of records returned when fetching results from the RecordService. If not set,
  // server default will be used.
  // TODO: It would be nice for the server to adjust this automatically based
  // on how fast the client is able to process records.
  public final static String FETCH_SIZE_CONF = "recordservice.task.fetch.size";

  // Optional configuration for the maximum memory the server will use per task.
  public final static String MEM_LIMIT_CONF = "recordservice.task.memlimit.bytes";

  // Optional configuration for the maximum number of records returned per task.
  public final static String RECORDS_LIMIT_CONF = "recordservice.task.records.limit";

  // Optional configuration for the maximum number of attempts to retry RecordService
  // RPCs with planner.
  public final static String PLANNER_RETRY_ATTEMPTS_CONF =
      "recordservice.planner.retry.attempts";

  // Optional configuration for sleep between retry attempts with planner.
  public final static String PLANNER_RETRY_SLEEP_MS_CONF =
      "recordservice.planner.retry.sleepMs";

  // Optional configuration for timeout on the planner service socket.
  public final static String PLANNER_SOCKET_TIMEOUT_MS_CONF =
      "recordservice.planner.socket.timeoutMs";

  // Optional configuration for the maximum number of attempts to retry RecordService
  // RPCs with worker.
  public final static String TASK_RETRY_ATTEMPTS_CONF =
      "recordservice.task.retry.attempts";

  // Optional configuration for sleep between retry attempts with worker.
  public final static String TASK_RETRY_SLEEP_MS_CONF =
      "recordservice.task.retry.sleepMs";

  // Optional configuration for timeout on the worker service socket.
  public final static String TASK_SOCKET_TIMEOUT_MS_CONF =
      "recordservice.task.socket.timeoutMs";

  // Optional configuration to enable server logging (logging level from log4j)
  public final static String TASK_ENABLE_SERVER_LOGGING_CONF =
      "recordservice.task.server.enableLogging";

  // Default value for the maximum number of attempts to retry RecordService RPCs
  // with planner.
  public final static int DEFAULT_PLANNER_RETRY_ATTEMPTS = 3;

  // Default value for sleep between retry attempts with planner.
  public final static int DEFAULT_PLANNER_RETRY_SLEEP_MS = 1000;

  // Default value for timeout on the planner service socket.
  public final static int DEFAULT_PLANNER_SOCKET_TIMEOUT_MS = 60000;

  // Default value for the maximum number of attempts to retry RecordService RPCs
  // with worker.
  public final static int DEFAULT_TASK_RETRY_ATTEMPTS = 3;

  // Default value for sleep between retry attempts with worker.
  public final static int DEFAULT_TASK_RETRY_SLEEP_MS = 1000;

  // Default value for timeout on the worker service socket.
  public final static int DEFAULT_TASK_SOCKET_TIMEOUT_MS = 10000;

  /**
   * Sets the input configuration to read 'cols' from 'db.tbl'. If the tbl is fully
   * qualified, db should be null.
   * If cols is empty, all cols in the table are read.
   */
  public static void setInputTable(Configuration config, String db, String tbl,
      String... cols) {
    if (tbl == null || tbl == "") {
      throw new IllegalArgumentException("'tbl' must be non-empty");
    }
    if (db != null && db != "") tbl = db + "." + tbl;
    config.set(TBL_NAME_CONF, tbl);
    if (cols != null && cols.length > 0) {
      for (int i = 0; i < cols.length; ++i) {
        if (cols[i] == null || cols[i].isEmpty()) {
          throw new IllegalArgumentException("Column list cannot contain empty names.");
        }
      }
      config.setStrings(COL_NAMES_CONF, cols);
    }
  }

  /**
   * Sets the input configuration to read results from 'query'.
   */
  public static void setInputQuery(Configuration config, String query) {
    if (query == null)  throw new IllegalArgumentException("'query' must be non-null");
    config.set(RecordServiceConfig.QUERY_NAME_CONF, query);
  }

  /**
   * Set the array of {@link Path}s as the list of inputs
   * for the map-reduce job.
   */
  public static void setInputPaths(Configuration conf,
      Path... inputPaths) throws IOException {
    Path path = inputPaths[0].getFileSystem(conf).makeQualified(inputPaths[0]);
    StringBuffer str = new StringBuffer(StringUtils.escapeString(path.toString()));
    for(int i = 1; i < inputPaths.length; ++i) {
      str.append(StringUtils.COMMA_STR);
      path = inputPaths[i].getFileSystem(conf).makeQualified(inputPaths[i]);
      str.append(StringUtils.escapeString(path.toString()));
    }
    conf.set("mapred.input.dir", str.toString());
  }


  /**
   * Returns a random hostport from plannerHostPortsStr. Each hostport should be
   * comma separated.
   */
  public static List<NetworkAddress> getPlannerHostPort(String plannerHostPortsStr)
      throws IOException {
    String[] plannerHostPorts = plannerHostPortsStr.trim().split(",");
    if (plannerHostPorts.length == 0) {
      throw new IOException("Invalid planner host port list: " + plannerHostPortsStr);
    }
    List<NetworkAddress> result = new ArrayList<NetworkAddress>();
    for (String hostPortStr: plannerHostPorts) {
      if (hostPortStr.length() == 0) continue;
      String[] hostPort = hostPortStr.split(":");
      if (hostPort.length != 2) {
        throw new IOException("Invalid hostport: " + hostPort);
      }
      String host = hostPort[0];
      int port = 0;
      try {
        port = Integer.parseInt(hostPort[1]);
      } catch (NumberFormatException e) {
        throw new IOException("Invalid hostport: " + hostPort);
      }
      result.add(new NetworkAddress(host, port));
    }
    return result;
  }
}
