/*
 * Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.
 *
 *
 * Except as expressly permitted in a written Agreement between your
 * company and Hortonworks, Inc, any use, reproduction, modification,
 * redistribution or other exploitation of all or any part of the contents
 * of this file is strictly prohibited.
 */
package org.hw.knox.system;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hw.knox.KnoxKeys;
import org.hw.knox.NonHATestBase;
import org.hw.knox.Request;
import org.hw.knox.Response;
import org.hw.knox.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class TestHDFS extends NonHATestBase {

  @Test
  public void testBasicJsonUseCase() throws Exception {
    /* Create a directory.
     curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=MKDIRS[&permission=<OCTAL>]"

     The client receives a respond with a boolean JSON object:
     HTTP/1.1 HttpStatus.SC_OK OK
     Content-Type: application/json
     Transfer-Encoding: chunked

     {"boolean": true}
     */
    String dirRoot = "/tmp/TestKnoxHDFS/testBasicJsonUseCase";

    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "MKDIRS" );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot + "/dir" );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    assertEquals( Boolean.TRUE, new JSONObject( new String( response.getContent() ) ).getBoolean( "boolean" ) );

    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "DELETE" );
    queryParams.put( "recursive", "true" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    response = session.delete( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
  }

  @Test
  public void testBasicOutboundHeaderUseCase() throws Exception {
    String dirRoot = "/tmp/TestKnoxHDFS/testBasicOutboundHeaderUseCase";

    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "CREATE" );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot + "/dir/file" );
    request.setQueryParams( queryParams );
    Response response = session.put( request );

    assertEquals( HttpStatus.SC_TEMPORARY_REDIRECT, response.getStatusCode() );

    String location = response.getHeaderValue( "Location" );

    assertNotNull( location );
    MatcherAssert.assertThat( location, startsWith( baseUrl ) );
    MatcherAssert.assertThat( location, containsString( "?_=" ) );
    MatcherAssert.assertThat( location, not( containsString( "host=" ) ) );
    MatcherAssert.assertThat( location, not( containsString( "port=" ) ) );
  }

  @Test
  public void testBasicUseCase() throws Exception {
    String dirRoot = "/tmp/TestKnoxHDFS/testBasicUseCase";

    // Attempt to delete the test directory in case a previous run failed.
    // Ignore any result.
    // Cleanup anything that might have been leftover because the test failed previously.
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "DELETE" );
    queryParams.put( "recursive", "true" );

    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    Response response = session.delete( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );

    /* Create a directory.
     curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=MKDIRS[&permission=<OCTAL>]"

     The client receives a respond with a boolean JSON object:
     HTTP/1.1 HttpStatus.SC_OK OK
     Content-Type: application/json
     Transfer-Encoding: chunked

     {"boolean": true}
     */
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "MKDIRS" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot + "/dir" );
    request.setQueryParams( queryParams );
    response = session.put( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    assertEquals( Boolean.TRUE, new JSONObject( new String( response.getContent() ) ).getBoolean( "boolean" ) );

    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "LISTSTATUS" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    response = session.get( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );

    JSONObject rootJson = new JSONObject( new String( response.getContent() ) ).getJSONObject( "FileStatuses" );
    assertNotNull( rootJson );
    JSONArray fileStatuses = rootJson.getJSONArray( "FileStatus" );
    assertNotNull( fileStatuses );
    assertTrue( fileStatuses.length() == 1 );
    JSONObject fileStatus = fileStatuses.getJSONObject( 0 );
    //assertEquals( "knox", fileStatus.getString( "owner" ) );//TODO: should be bob
    assertEquals( "DIRECTORY", fileStatus.getString( "type" ) );
    assertEquals( "hdfs", fileStatus.getString( "group" ) );
    assertEquals( "dir", fileStatus.getString( "pathSuffix" ) );
    assertEquals( "755", fileStatus.getString( "permission" ) );

    //System.out.println( rootJson );

    //NEGATIVE: Test a bad password.
    Session tmpSession = Session.login( baseUrl, KnoxKeys.USER_NAME, "invalid-password" );
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "LISTSTATUS" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    response = tmpSession.get( request );

    assertEquals( HttpStatus.SC_UNAUTHORIZED, response.getStatusCode() );

    //NEGATIVE: Test a bad user.
    tmpSession = Session.login( baseUrl, "invalid-user", KnoxKeys.USER_PASSWORD );
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "LISTSTATUS" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    response = tmpSession.get( request );

    assertEquals( HttpStatus.SC_UNAUTHORIZED, response.getStatusCode() );

    /* Add a file.
     curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=CREATE
     [&overwrite=<true|false>][&blocksize=<LONG>][&replication=<SHORT>]
     [&permission=<OCTAL>][&buffersize=<INT>]"

     The expect is redirected to a datanode where the file data is to be written:
     HTTP/1.1 307 TEMPORARY_REDIRECT
     Location: http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=CREATE...
     Content-Length: 0

     Step 2: Submit another HTTP PUT expect using the URL in the Location header with the file data to be written.
     curl -i -X PUT -T <LOCAL_FILE> "http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=CREATE..."

     The client receives a HttpStatus.SC_CREATED Created respond with zero content length and the WebHDFS URI of the file in the Location header:
     HTTP/1.1 HttpStatus.SC_CREATED Created
     Location: webhdfs://<HOST>:<PORT>/<PATH>
     Content-Length: 0
     */
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "CREATE" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot + "/dir/file" );
    request.setQueryParams( queryParams );
    response = session.put( request );

    assertEquals( HttpStatus.SC_TEMPORARY_REDIRECT, response.getStatusCode() );

    String location = response.getHeaderValue( "Location" );

    assertNotNull( location );
    MatcherAssert.assertThat( location, startsWith( baseUrl ) );
    MatcherAssert.assertThat( location, containsString( "?_=" ) );
    MatcherAssert.assertThat( location, not( containsString( "host=" ) ) );
    MatcherAssert.assertThat( location, not( containsString( "port=" ) ) );

    String content = "TEST";

    request = new Request();
    request.setPath( location );
    request.setContentString( content );
    request.setContentType( "text/plain" );
    response = session.put( request );

    assertEquals( HttpStatus.SC_CREATED, response.getStatusCode() );

    location = response.getHeaderValue( "Location" );
    MatcherAssert.assertThat( location, startsWith( baseUrl ) );

    /* Get the file.
     curl -i -L "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=OPEN
     [&offset=<LONG>][&length=<LONG>][&buffersize=<INT>]"

     The expect is redirected to a datanode where the file data can be read:
     HTTP/1.1 307 TEMPORARY_REDIRECT
     Location: http://<DATANODE>:<PORT>/webhdfs/v1/<PATH>?op=OPEN...
     Content-Length: 0

     The client follows the redirect to the datanode and receives the file data:
     HTTP/1.1 HttpStatus.SC_OK OK
     Content-Type: application/octet-stream
     Content-Length: 22

     TEST
     */
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "OPEN" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot + "/dir/file" );
    request.setQueryParams( queryParams );
    response = session.get( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    assertEquals( content, new String( response.getContent() ) );

    /* Delete the directory.
     curl -i -X DELETE "http://<host>:<port>/webhdfs/v1/<path>?op=DELETE
     [&recursive=<true|false>]"

     The client receives a respond with a boolean JSON object:
     HTTP/1.1 HttpStatus.SC_OK OK
     Content-Type: application/json
     Transfer-Encoding: chunked

     {"boolean": true}
     */
    queryParams = new HashMap<String, String>();
    queryParams.put( "op", "DELETE" );
    queryParams.put( "recursive", "true" );

    request = new Request();
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + dirRoot );
    request.setQueryParams( queryParams );
    response = session.delete( request );

    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    assertEquals( Boolean.TRUE, new JSONObject( new String( response.getContent() ) ).getBoolean( "boolean" ) );
  }
}
