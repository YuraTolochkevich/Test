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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.hw.knox.util.KnoxUtils;
import org.junit.BeforeClass;

public abstract class HATestBase extends KnoxTestBase {

  // Default Setup method that each class can use
  @BeforeClass
  public static void setUp() throws Exception {
    initSession();

  }

  private static void initSession() throws Exception {
    StringBuilder url = new StringBuilder()
        .append( KnoxKeys.HA_SCHEME ).append( "://" )
        .append( KnoxKeys.HA_HOST );
    if ( KnoxKeys.HA_PORT != null && !KnoxKeys.HA_PORT.isEmpty() ) {
      url.append( ":" ).append( KnoxKeys.HA_PORT );
    }
    url.append( KnoxKeys.HA_CONTEXT );
    baseUrl = url.toString();
    session = Session.login( baseUrl, KnoxKeys.USER_NAME, KnoxKeys.USER_PASSWORD );
  }
    protected static void initSessionPool(int sessionsNumber) throws Exception {
        StringBuilder url = new StringBuilder()
                .append( KnoxKeys.HA_SCHEME ).append( "://" )
                .append( KnoxKeys.HA_HOST );
        if ( KnoxKeys.HA_PORT != null && !KnoxKeys.HA_PORT.isEmpty() ) {
            url.append( ":" ).append( KnoxKeys.HA_PORT );
        }
        url.append( KnoxKeys.HA_CONTEXT );
        baseUrl = url.toString();
        for (int i =0;i<sessionsNumber;i++)
            sessionPool.add(Session.login( baseUrl, KnoxKeys.USER_NAME, KnoxKeys.USER_PASSWORD ));
    }

  protected static void reopenSession() throws Exception {
    session.close();
    initSession();
  }

  protected HAKnox whoReceivedRequest( String requestPattern ) throws IOException {
    for ( HAKnox haKnox : HAKnox.values() ) {
      if ( KnoxUtils.knoxReceivedRequest( haKnox, requestPattern ) ) {
        return haKnox;
      }
    }
    return null;
  }

  protected void assertKnoxProcessedRequest( HAKnox worker, String requestString ) throws IOException {
    boolean knoxReceivedRequest;
    for ( HAKnox haKnox : HAKnox.values() ) {
      knoxReceivedRequest = KnoxUtils.knoxReceivedRequest( haKnox, requestString );
      if ( haKnox == worker ) {
        assertTrue( "Knox instance " + haKnox.name() + " had to process the request", knoxReceivedRequest );
      } else {
        assertFalse( "Knox instance " + haKnox.name() + " hadn't to process the request", knoxReceivedRequest );
      }
    }
  }
}
