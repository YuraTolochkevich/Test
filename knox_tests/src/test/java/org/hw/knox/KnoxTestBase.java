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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class KnoxTestBase {

  // instantiate the logger
  protected static final Logger LOG = Logger.getLogger( KnoxTestBase.class );
  protected static String baseUrl;
  protected static Session session;
    public static List<Session> sessionPool = new ArrayList<Session>();
  // Default Setup method that each class can use
  @BeforeClass
  public static void setUp() throws Exception {
    // configure log4j
    PropertyConfigurator.configure( KnoxKeys.LOG4J_FILE );
    LOG.info( "Hadoop Version :" + KnoxKeys.HADOOP_VERSION );
  }

  // Default tear down that each class can use
  @AfterClass
  public static void tearDown() throws Exception {
  }

  protected String ls( String file, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "LISTSTATUS" );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.get(request );

    assertEquals( status, response.getStatusCode() );

    return new String( response.getContent() );
  }
    protected String lsWithPool( String file, int status, int i ) throws Exception {
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put( "op", "LISTSTATUS" );

        Request request = new Request();
        request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
        request.setQueryParams( queryParams );
        Response response = sessionPool.get(i).get(request);

        assertEquals( status, response.getStatusCode() );

        return new String( response.getContent() );
    }

  protected void deleteFile( String file, String recursive, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "DELETE" );
    queryParams.put( "recursive", recursive );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.delete( request );

    assertEquals( status, response.getStatusCode() );
  }

  protected void deleteFile( String file, String recursive) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "DELETE" );
    queryParams.put( "recursive", recursive );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.delete( request );
  }

  protected void createDir( String dir, String permission, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "MKDIRS" );
    if (permission != null) {
    	queryParams.put( "permission", permission );
    }
    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dir );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( status, response.getStatusCode() );
    assertEquals( Boolean.TRUE, new JSONObject( new String( response.getContent() ) ).getBoolean( "boolean" ) );
  }

  protected void chownFile( String file, String owner, String group, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "SETOWNER" );
    queryParams.put( "owner", owner );
    queryParams.put( "group", group );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( status, response.getStatusCode() );
  }

  protected void createDir( String group, String owner, String dir, String permission,
      int nnStatus, int chownStatus ) throws Exception {
    createDir( dir, permission, nnStatus );
    chownFile( dir, owner, group, chownStatus );
  }

  protected String createFile( String file, String permission, String contentType, File resource,
      int nnStatus, int dnStatus, int chmodStatus ) throws Exception {
    String location = createFileNN( file, permission, nnStatus );
    if ( location != null ) {
      int status = createFileDN( location, contentType, resource, dnStatus );
      if ( status < 300 && permission != null ) {
        chmodFile( file, permission, chmodStatus );
      }
    }
    return location;
  }

  protected String createFileNN( String file, String permission, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "CREATE" );
    if (permission != null) {
    	queryParams.put( "permission", permission );
    }
    
    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( status, response.getStatusCode() );
    return response.getHeaderValue( "Location" );
  }

  protected int createFileDN( String location, String contentType, File resource, int status ) throws Exception {
    Request request = new Request();
    request.setPath( location );
    request.setContentType( contentType );
    request.setContentFile( resource );
    Response response = session.put( request );

    assertEquals( status, response.getStatusCode() );
    return response.getStatusCode();
  }

  public void chmodFile( String file, String permission, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "SETPERMISSION" );
    queryParams.put( "permission", permission );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( status, response.getStatusCode() );
  }

  public String submitJava( String jar, String main, String input, String output, int status ) throws Exception {
    Multimap<String, String> formParams = ArrayListMultimap.create();
    formParams.put( "jar", jar );
    formParams.put( "class", main );
    formParams.put( "arg", input );
    formParams.put( "arg", output );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.TEMPLETON_PATH + "/mapreduce/jar" );
    request.setContentType( "application/x-www-form-urlencoded" );
    request.setFormParams( formParams );
    Response response = session.post( request );

//    String job = from( json ).getString( "id" );
//   log.debug( "JOB=" + job );
    System.out.println( new String( response.getContent() ) );
    System.out.println( response.getStatusCode() );
    return null;
  }

  protected byte[] downloadFile( String file, int status ) throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "OPEN" );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file );
    request.setQueryParams( queryParams );
    Response response = session.get( request );

    assertEquals( status, response.getStatusCode() );

    return response.getContent();
  }
}
