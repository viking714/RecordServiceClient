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

package com.cloudera.recordservice.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.mapred.InputSplit;

public class RecordServiceInputSplit implements InputSplit{

  private com.cloudera.recordservice.mapreduce.RecordServiceInputSplit split_ =
      null;

  public RecordServiceInputSplit() {
  }

  public RecordServiceInputSplit(
      com.cloudera.recordservice.mapreduce.RecordServiceInputSplit split) {
    this.split_ = split;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    split_.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    this.split_ =
        new com.cloudera.recordservice.mapreduce.RecordServiceInputSplit();
    this.split_.readFields(in);
  }

  @Override
  public long getLength() throws IOException {
    try {
      return this.split_.getLength();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String[] getLocations() throws IOException {
    try {
      return this.split_.getLocations();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  public com.cloudera.recordservice.mapreduce.RecordServiceInputSplit
      getBackingSplit() {
    return split_;
  }
}
