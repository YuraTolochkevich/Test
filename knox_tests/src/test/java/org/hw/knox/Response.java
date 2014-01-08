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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;

public class Response {

  private Multimap<String, String> headers = ArrayListMultimap.create();
  private int statusCode;
  private byte[] content;

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode( int statusCode ) {
    this.statusCode = statusCode;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent( byte[] content ) {
    this.content = content;
  }

  public void addHeader( String name, String value ) {
    headers.put( name, value );
  }

  public String getHeaderValue( String name ) {
    Collection<String> values = headers.get( name );
    if ( values == null || values.isEmpty() ) {
      return null;
    }
    return values.iterator().next();
  }
}
