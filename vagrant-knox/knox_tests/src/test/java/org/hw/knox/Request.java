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

import com.google.common.collect.Multimap;

import java.io.File;
import java.util.Map;

public class Request {

  private String path;
  private Map<String, String> queryParams;
  private Multimap <String, String> formParams;
  private String contentType = "application/json";
  private byte[] contentBytes;
  private String contentString;
  private File contentFile;
  private String accept = "application/json";

  public Request() {
  }

  public String getPath() {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public void setQueryParams( Map<String, String> queryParams ) {
    this.queryParams = queryParams;
  }

  public Multimap <String, String> getFormParams() {
    return formParams;
  }

  public void setFormParams( Multimap <String, String> formParams ) {
    this.formParams = formParams;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType( String contentType ) {
    this.contentType = contentType;
  }

  public byte[] getContentBytes() {
    return contentBytes;
  }

  public void setContentBytes( byte[] contentBytes ) {
    this.contentBytes = contentBytes;
  }

  public String getContentString() {
    return contentString;
  }

  public void setContentString( String contentString ) {
    this.contentString = contentString;
  }

  public File getContentFile() {
    return contentFile;
  }

  public void setContentFile( File contentFile ) {
    this.contentFile = contentFile;
  }

  public String getAccept() {
    return accept;
  }

  public void setAccept( String accept ) {
    this.accept = accept;
  }
}
