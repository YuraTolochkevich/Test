/*
 * Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.
 *
 *
 * Except as expressly permitted in a written Agreement between your
 * company and Hortonworks, Inc, any use, reproduction, modification,
 * redistribution or other exploitation of all or any part of the contents
 * of this file is strictly prohibited.
 */
package org.hw.knox.stress;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.hw.knox.KnoxKeys;
import org.hw.knox.NonHATestBase;
import org.hw.knox.Request;
import org.hw.knox.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class TestHBase extends NonHATestBase {
  private static final int HBASE_LARGE_REQUEST_SIZE = 1024 * 1024 * 1024; // 1GB
  private static final String TEMPORARY_DIR_PATH = "/tmp/KnoxStressTest";

  @Test
  public void testHbaseLargeRequestResponse() throws Exception {
    String tableName = "testHbaseLargeRequestResponse";
    String requestFileName = "testHbaseLargeRequestResponse";
    String encodedColumnAttribute = getBase64EncodedString( "family1:column1" );
    String tableSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><TableSchema name=\""
        + tableName + "\"><ColumnSchema name=\"family1\"/></TableSchema>";
    String data = new String( new char[1000] ).replace( '\0', 't' );
    String encodedCellData = getBase64EncodedString( data );

    Response response = null;

    //Recreate test table in HBase
    deleteTable( tableName );
    createTable( tableName, tableSchema );
    
    //Recreate temporary directory
    File tempDirectory = new File( TEMPORARY_DIR_PATH );
    FileUtils.deleteQuietly( tempDirectory );
    FileUtils.forceMkdir( tempDirectory );
    
    //Generate multi-row insert request with specified size
    File requestFile = new File( TEMPORARY_DIR_PATH + "/" + requestFileName );
    FileWriter fw = new FileWriter( requestFile, true );
    fw.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><CellSet>" );
    for( int i = 0; requestFile.length() < HBASE_LARGE_REQUEST_SIZE; i++ ) {
      String rowIndex = "test_row_number_" + i;
      fw.write( "<Row key=\"" + getBase64EncodedString( rowIndex ) + "\">" );
      fw.write( "<Cell column=\"" + encodedColumnAttribute + "\">"
          + encodedCellData + "</Cell></Row>" );
    }
    fw.write( "</CellSet>" );
    fw.close();
    
    //Send multi-row request
    Request insertDataRequest = new Request();
    insertDataRequest.setContentFile( requestFile );
    insertDataRequest.setContentType( "text/xml" );
    insertDataRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/false-row-key" );
    response = session.post( insertDataRequest );
    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );

    //Get inserted data
    Request getDataRequest = new Request();
    getDataRequest.setAccept( "text/xml" );
    getDataRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/*" );
    response = session.get( getDataRequest );
    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    
    //Drop test table
    deleteTable( tableName );

    FileUtils.forceDelete( tempDirectory );
  }

  @Test
  public void testHBaseConcurrentDataReads() throws Exception {
    String tableName = "testHBaseConcurrentDataReads";
    String encodedColumnAttribute = getBase64EncodedString( "family1:column1" );
    String tableSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><TableSchema name=\""
        + tableName + "\"><ColumnSchema name=\"family1\"/></TableSchema>";
    String encodedCellData = getBase64EncodedString( "test" );
    long rowsNumber = 1000; //number of rows in test table
    int threadsNum = 100;
    
    //Recreate test table in HBase
    deleteTable( tableName );
    createTable( tableName, tableSchema );
    
    StringBuilder insertData = new StringBuilder( "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CellSet>" );
    for ( int i = 0; i < rowsNumber; i++ ) {
      insertData.append( "<Row key=\"" + getBase64EncodedString( "row" + i ) + "\">" );
      insertData.append( "<Cell column=\"" + encodedColumnAttribute + "\">"
          + encodedCellData + "</Cell></Row>" );
    }
    insertData.append( "</CellSet>" );
    
    Request insertDataRequest = new Request();
    insertDataRequest.setContentString( insertData.toString() );
    insertDataRequest.setContentType( "text/xml" );
    insertDataRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/false-row-key" );
    Response response = session.post( insertDataRequest );
    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    
    executeConcurrentDataReads( tableName, threadsNum, rowsNumber );
    
    //Drop test table
    deleteTable( tableName );
  }

  private String getBase64EncodedString(String input) {
    return new String( Base64.encodeBase64( input.getBytes() ) );
  }

  private void executeConcurrentDataReads( final String tableName, int threadsNum, final long rowsNumber ) throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    final AtomicLong totalRowsRecived = new AtomicLong();
    List<Callable<Void>> callables = new ArrayList<Callable<Void>>( threadsNum );
    long expectedRowNumber = threadsNum * rowsNumber;
    try {
      
      for ( int i = 0; i < threadsNum; i++ ) {
        callables.add( new Callable<Void>() {
          public Void call() throws Exception {
            
            Request getDataRequest = new Request();
            getDataRequest.setAccept( "text/xml" );
            getDataRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/*" );
            Response response = session.get( getDataRequest );
            assertEquals( HttpStatus.SC_OK, response.getStatusCode() );            
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String responseContent = new String( response.getContent() );
            InputSource is = new InputSource( new StringReader( responseContent ) );
            Document doc = builder.parse( is );
            XPathFactory xpathfactory = XPathFactory.newInstance();
            XPath xpath = xpathfactory.newXPath();
            XPathExpression expr = xpath.compile( "count(//CellSet/Row)" );
            long rowsInResponse = ( (Double) expr.evaluate( doc, XPathConstants.NUMBER ) ).longValue();
            assertEquals( rowsNumber, rowsInResponse );
            totalRowsRecived.addAndGet( rowsInResponse );
            return null;
          }
        } );
        
      }
      executor.invokeAll( callables );
    } finally {
      executor.shutdown();
      assertEquals( expectedRowNumber, totalRowsRecived.get() );
    }
  }

  private void deleteTable( String tableName ) throws Exception {
    Request getTablesRequest = new Request();
    getTablesRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH );
    Response response = session.get( getTablesRequest );
    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    JSONArray tables = new JSONObject( new String( response.getContent() ) ).optJSONArray( "table" );
    for( int i = 0; i < tables.length(); i++ ) {
      JSONObject table = tables.getJSONObject( i );
      if( tableName.equals( table.getString( "name" ) ) ) {
        Request deleteTableRequest = new Request();
        deleteTableRequest.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/schema" );
        response = session.delete( deleteTableRequest );
        assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
        break;
      }
    }
  }

  private void createTable( String tableName, String xmlSchema ) throws Exception {
    Request request = new Request();
    request.setPath( baseUrl + KnoxKeys.HBASE_PATH + "/" + tableName + "/schema" );
    request.setContentString( xmlSchema );
    request.setContentType( "text/xml" );
    Response response = session.post( request );
    assertEquals( HttpStatus.SC_CREATED, response.getStatusCode() );
  }
}
