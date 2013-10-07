/**
 * (c) Copyright 2013 WibiData, Inc.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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

package org.kiji.scoring;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import org.kiji.annotations.ApiAudience;
import org.kiji.annotations.ApiStability;
import org.kiji.schema.KijiColumnName;
import org.kiji.schema.KijiTable;
import org.kiji.scoring.impl.InternalFreshKijiTableReader;

/**
 * Builder for configuring options for FreshKijiTableReaders.
 *
 * <p>
 *   Allows the setting of options for creation of FreshKijiTableReaders.  Options include setting
 *   the reader type as enumerated in FreshKijiTableReaderBuilder.FreshReaderType (defaults to
 *   local), setting the time (in milliseconds) to wait for freshening to occur (defaults to 100
 *   ms), setting the period (in milliseconds) between automatically rereading freshness policies
 *   from the meta table (defaults to never automatically rereading), setting whether to allow
 *   partially fresh data to be returned by calls to {@link org.kiji.scoring.FreshKijiTableReader
 *   #get(org.kiji.schema.EntityId, org.kiji.schema.KijiDataRequest)} (defaults to false), and a
 *   required setting for the table from which to read.
 * </p>
 *
 * <p>
 *   To create a new FreshKijiTableReader:
 * </p>
 * <p><pre>
 *   final FreshKijiTableReader = FreshKijiTableReaderBuilder.create()
 *       .withTable(myTable)
 *       .withTimeout(100)
 *       .withAutomaticReread(3600000)
 *       .returnPartialFreshData(true)
 *       build();
 * </pre></p>
 */
@ApiAudience.Public
@ApiStability.Experimental
public final class FreshKijiTableReaderBuilder {
  /** By default, do not allow returning partially fresh data. */
  private static final Boolean DEFAULT_PARTIAL_FRESHENING = false;
  /** By default, Wait 100 milliseconds for freshening to occur. */
  private static final long DEFAULT_TIMEOUT = 100;
  /** By default, do not automatically reread. */
  private static final long DEFAULT_REREAD_PERIOD = 0;
  /** By default, freshen all columns. */
  private static final List<KijiColumnName> DEFAULT_COLUMNS_TO_FRESHEN = Collections.emptyList();

  /**
   * Get a new instance of FreshKijiTableReaderBuilder.
   *
   * @return a new instance of FreshKijiTableReaderBuilder.
   */
  public static FreshKijiTableReaderBuilder create() {
    return new FreshKijiTableReaderBuilder();
  }

  /** The KijiTable from which the new reader will read. */
  private KijiTable mTable = null;
  /**
   * The time in milliseconds the new reader will wait for freshening to occur. Default is 100
   * milliseconds.
   */
  private Long mTimeout = null;
  /**
   * The time in milliseconds the new reader will wait between automatically rereading Freshener
   * records from the meta table. Default is to not automatically reread.
   */
  private Long mRereadPeriod = null;
  /**
   * Whether or not the new reader will return and commit partially fresh data when available.
   * Default is to not allow partial freshening.
   */
  private Boolean mAllowPartialFresh = null;
  /** Specifies which columns to freshen.  Default is all columns. */
  private List<KijiColumnName> mColumnsToFreshen = null;

  /**
   * Configure the FreshKijiTableReader to read from the given KijiTable.
   *
   * @param table the KijiTable from which to read.
   * @return this FreshKijiTableReaderBuilder configured to read from the given table.
   */
  public FreshKijiTableReaderBuilder withTable(KijiTable table) {
    Preconditions.checkState(null == mTable, "KijiTable already set to: %s", mTable);
    mTable = table;
    return this;
  }

  /**
   * Configure the FreshKijiTableReader to wait a given number of milliseconds before returning
   * stale data.
   *
   * @param timeout the default duration in milliseconds to wait before returning stale data. This
   *     may be overriden at request time by using
   *     {@link FreshKijiTableReader#get(org.kiji.schema.EntityId, org.kiji.schema.KijiDataRequest,
   *     long)}.
   * @return this FreshKijiTableReaderBuilder configured to wait the given number of milliseconds
   * before returning stale data.
   */
  public FreshKijiTableReaderBuilder withTimeout(long timeout) {
    Preconditions.checkArgument(0 < timeout, "Timeout must be positive, got: %d", timeout);
    Preconditions.checkState(null == mTimeout, "Timeout is already set to: %d", mTimeout);
    mTimeout = timeout;
    return this;
  }

  /**
   * Configure the FreshKijiTableReader to automatically reread freshness policies from the meta
   * table on a scheduled interval.
   *
   * @param rereadPeriod the interval between automatic rereads in milliseconds.  rereadPeriod may
   * not be negative and a rereadPeriod value of 0 indicate never automatically rereading freshness
   * policies from the metatable.
   * @return this FreshKijiTableReaderBuilder configured to automatically reread on the given
   * interval.
   */
  public FreshKijiTableReaderBuilder withAutomaticReread(long rereadPeriod) {
    Preconditions.checkArgument(
        0 < rereadPeriod, "Reread time must be positive, got: %s", rereadPeriod);
    Preconditions.checkState(
        null == mRereadPeriod, "Reread time is already set to: %d", mRereadPeriod);
    mRereadPeriod = rereadPeriod;
    return this;
  }

  /**
   * Configure the FreshKijiTableReader to return partially fresh data when available.  This
   * option may increase the time to return for certain calls to
   * {@link FreshKijiTableReader#get(org.kiji.schema.EntityId, org.kiji.schema.KijiDataRequest)}.
   * If set to true, each producer will create its own table connection and all producer writes will
   * be committed atomically when produce() returns.  If set to false, all producers for a single
   * request will share a table connection and all writes will be cached until all producers for
   * that request have returned.
   *
   * @param allowPartial whether the FreshKijiTableReader should return partially freshened data
   * when available.  If set to true, each producer will create its own table connection and all
   * producer writes will be committed atomically when produce() returns.  If set to false, all
   * producers for a single request will share a table connection and all writes will be cached
   * until all producers for that request have returned.
   * @return this FreshKijiTableReaderBuilder configured to allow returning partially freshened
   * data.
   */
  public FreshKijiTableReaderBuilder withPartialFreshening(boolean allowPartial) {
    Preconditions.checkState(
        null == mAllowPartialFresh, "Partial freshening is already set to: %s", mAllowPartialFresh);
    mAllowPartialFresh = allowPartial;
    return this;
  }

  /**
   * Configure the FreshKijiTableReader to only freshen requests for a specific set of columns.
   *
   * <ul>
   *   <li>Specifying a qualified column will enable the Freshener for that column only.</li>
   *   <li>Specifying a map type family will enable the Freshener for that map type family if one
   *     exists.</li>
   *   <li>Specifying any family will enable all Fresheners for qualified columns within that
   *     family.</li>
   *   <li>Specifying a qualified column in a map type family when there is a Freshener attached to
   *     the entire family will not enable the full family Freshener.</li>
   * </ul>
   *
   * @param columnsToFreshen the columns which the reader should freshen.
   * @return this FreshKijiTableReaderBuilder configured to read from a specific set of columns.
   */
  public FreshKijiTableReaderBuilder withColumnsToFreshen(List<KijiColumnName> columnsToFreshen) {
    if (null != mColumnsToFreshen) {
      final String columns = Joiner.on(", ").join(mColumnsToFreshen);
      throw new IllegalStateException(
          String.format("Columns to freshen are already set to: %s", columns));
    } else {
      mColumnsToFreshen = columnsToFreshen;
    }
    return this;
  }

  /**
   * Builds a FreshKijiTableReader with the configured options.
   *
   * @return a FreshKijiTableReader with the configured options.
   * @throws IOException in case of an error creating the FreshKijiTableReader.
   */
  public FreshKijiTableReader build() throws IOException {
    Preconditions.checkState(mTable != null, "Target table must be set in order to build.");
    if (null == mTimeout) {
      mTimeout = DEFAULT_TIMEOUT;
    }
    if (null == mAllowPartialFresh) {
      mAllowPartialFresh = DEFAULT_PARTIAL_FRESHENING;
    }
    if (null == mRereadPeriod) {
      mRereadPeriod = DEFAULT_REREAD_PERIOD;
    }
    if (null == mColumnsToFreshen) {
      mColumnsToFreshen = DEFAULT_COLUMNS_TO_FRESHEN;
    }

    return new InternalFreshKijiTableReader(
            mTable, mTimeout, mRereadPeriod, mAllowPartialFresh, mColumnsToFreshen);
  }
}
