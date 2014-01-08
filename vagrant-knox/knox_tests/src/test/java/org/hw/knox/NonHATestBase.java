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

import org.junit.BeforeClass;

public abstract class NonHATestBase extends KnoxTestBase {

  // Default Setup method that each class can use
  @BeforeClass
  public static void setUp() throws Exception {
    StringBuilder url = new StringBuilder()
        .append( KnoxKeys.KNOX_SCHEME ).append( "://" )
        .append( KnoxKeys.KNOX_HOST );
    if ( KnoxKeys.KNOX_PORT != null && !KnoxKeys.KNOX_PORT.isEmpty() ) {
      url.append( ":" ).append( KnoxKeys.KNOX_PORT );
    }
    url.append( KnoxKeys.KNOX_CONTEXT );
    baseUrl = url.toString();
    session = Session.login( baseUrl, KnoxKeys.USER_NAME, KnoxKeys.USER_PASSWORD );
  }
}
