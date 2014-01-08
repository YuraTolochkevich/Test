/*
 * Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.
 *
 *
 * Except as expressly permitted in a written Agreement between your
 * company and Hortonworks, Inc, any use, reproduction, modification,
 * redistribution or other exploitation of all or any part of the contents
 * of this file is strictly prohibited.
 */
package org.hw.knox;

public enum HAKnox {

  KNOX_A( KnoxKeys.KNOX_A_LOG_DIR, KnoxKeys.KNOX_A_HOME_DIR ),
  KNOX_B( KnoxKeys.KNOX_B_LOG_DIR, KnoxKeys.KNOX_B_HOME_DIR );
  public final String logDir;
  public final String homeDir;

  HAKnox( String logDir, String homeDir ) {
    this.logDir = logDir;
    this.homeDir = homeDir;
  }
}
