/*
 * Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.
 *
 *
 * Except as expressly permitted in a written Agreement between your
 * company and Hortonworks, Inc, any use, reproduction, modification,
 * redistribution or other exploitation of all or any part of the contents
 * of this file is strictly prohibited.
 */
package org.hw.knox.ha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.hw.knox.HAKnox;
import org.hw.knox.HATestBase;
import org.hw.knox.util.KnoxUtils;
import org.hw.knox.util.security.CMFMasterService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * All tests expect that Round Robin balancing algorithm is used.
 */
public class TestHA extends HATestBase {

  private static final int TEST_SESSION_STICKINESS_LOOP_COUNT = 20;
  private static final int TEST_LOAD_BALANCING_LOOP_COUNT = 43;
  private static final String LS_REQUEST_IN_LOG = ".*DEBUG hadoop\\.gateway: Received request: GET /webhdfs/v1/\\?op=LISTSTATUS";
  private static final String CREATE_FILE_ON_NN_REQUEST_IN_LOG_PATTERN = ".*DEBUG hadoop\\.gateway: Received request: PUT /webhdfs/v1/tmp/[a-zA-Z\\.]*\\?op=CREATE";
  private static final String CREATE_FILE_ON_DN_REQUEST_IN_LOG_PATTERN = ".*DEBUG hadoop\\.gateway: Received request: PUT /webhdfs/data/v1/webhdfs/v1/tmp/[a-zA-Z\\.]*\\?.*";
  private static final int HDFS_FILE_SIZE = 64 * 1024 * 1024; // 64 MB
  private static CMFMasterService service;

  @BeforeClass
  public static void haTestsSetUp() throws Exception {
    KnoxUtils.stopAllKnoxes();

    service = new CMFMasterService();
    KnoxUtils.setupAllKnoxesMasterSecret( service );
    KnoxUtils.setupAllKnoxes();

    KnoxUtils.startAllKnoxes();
    TimeUnit.SECONDS.sleep( 5 );
    KnoxUtils.stopAllKnoxes();
    TimeUnit.SECONDS.sleep( 5 );
    KnoxUtils.synchKeystores( HAKnox.KNOX_A );
    KnoxUtils.startAllKnoxes();
    TimeUnit.SECONDS.sleep( 5 );
      initSessionPool(TEST_LOAD_BALANCING_LOOP_COUNT);
  }

  /**
   * Clear log files of all Knox instances before each test call.
   * Log file with some data is used for marking instances that processed
   * client's request.
   */
  @Before
  public void beforeTest() throws Exception {
    KnoxUtils.clearAllKnoxLogs();

    // Apache HTTP Server's BalancerMember descriptor has an attribute 'retry'
    // with default value 60.
    //
    // Its meaning:
    // Connection pool worker retry timeout in seconds. If the connection pool
    // worker to the backend server is in the error state, Apache will not
    // forward any requests to that server until the timeout expires.
    //
    // So we need to sleep a little bit more than 60s so that Apache HTTP Server
    // will be able to try to send request to nodes which were shot down in a
    // previous test.
    TimeUnit.SECONDS.sleep( 70 );
  }

  /**
   * Scenario is following:
   * 1) execute first client's request;
   * 2) find out what Knox instance processed that request;
   * 3) execute client's request N times and verify that those request were passed
   *    to the same Knox instance. The other Knox instance must not receive any
   *    request from client.
   */
  @Test
  public void testSessionStickiness() throws Exception {
    ls( "/", HttpStatus.SC_OK );

    HAKnox worker = whoReceivedRequest( LS_REQUEST_IN_LOG );
    assertNotNull( worker );

    for ( int i = 0; i < TEST_SESSION_STICKINESS_LOOP_COUNT; i++ ) {
      KnoxUtils.clearAllKnoxLogs();
      ls( "/", HttpStatus.SC_OK );
      assertKnoxProcessedRequest( worker, LS_REQUEST_IN_LOG );
    }
  }

  /**
   * Scenario is following:
   * 1) execute client's request;
   * 2) find out what Knox instance processed that request;
   * 3) open new session;
   * 4) execute client's request and verify that that request was passed
   *    to the other Knox instance.
   * 5) repeat steps 3-4 N times
   */
  @Test
//  @Ignore( "It is ignored because 'reopenSession' doesn't help to create"
//      + " new session so that Apache HTTP server will send all requests to"
//      + " the same worker." )
  public void testLoadBalancing() throws Exception {
    lsWithPool( "/", HttpStatus.SC_OK,0);

    HAKnox currentWorker = whoReceivedRequest( LS_REQUEST_IN_LOG );
    assertNotNull( currentWorker );
     int count =0;
    for ( int i = 1; i < TEST_LOAD_BALANCING_LOOP_COUNT; i++ ) {
     // reopenSession();
      KnoxUtils.clearAllKnoxLogs();

      lsWithPool( "/", HttpStatus.SC_OK,i );
       //ls( "/", HttpStatus.SC_OK);
      //org.apache.hadoop.util.Shell 

      HAKnox previousWorker = currentWorker;
      currentWorker = whoReceivedRequest( LS_REQUEST_IN_LOG );
      assertNotNull( currentWorker );



     // assertNotSame( previousWorker, currentWorker );


    }
      System.out.println(count );
  }

  /**
   * Scenario is following:
   * 1) execute create file on NN request;
   * 2) find out what Knox instance processed that request;
   * 3) shoot down that instance;
   * 4) execute create file on DN request;
   * 5) verify that other Knox instance processed request successfully.
   * 
   * Also this test verifies that Knox B is able to decrypt query string which
   * was encrypted by Knox instance A.
   */
  @Test
  public void testFailoverWhenKnoxInstanceIsDownBeforeCleintRequestComesIn() throws Exception {
    assertTrue( "All Knox instances must run", KnoxUtils.allKnoxesAreRunning() );

    String filePath = "/tmp/testFailoverWhenKnoxInstanceIsDownBeforeCleintRequestComesIn.txt";
    File file = new File( filePath );

    deleteFile( filePath, "true");

    RandomAccessFile randomAccessFile = new RandomAccessFile( file, "rw" );
    randomAccessFile.setLength( 1024 );
    randomAccessFile.close();

    String location = createFileNN( filePath, null, HttpStatus.SC_TEMPORARY_REDIRECT );
    assertNotNull( location );

    HAKnox createFileOnNNWorker = whoReceivedRequest( CREATE_FILE_ON_NN_REQUEST_IN_LOG_PATTERN );
    assertNotNull( createFileOnNNWorker );

    boolean knoxWasStopped = KnoxUtils.stopKnox( createFileOnNNWorker );
    try {
      createFileDN( location, ContentType.DEFAULT_BINARY.getMimeType(), file, HttpStatus.SC_CREATED );

      HAKnox createFileOnDNWorker = whoReceivedRequest( CREATE_FILE_ON_DN_REQUEST_IN_LOG_PATTERN );
      assertNotNull( createFileOnDNWorker );
      assertNotSame( createFileOnNNWorker, createFileOnDNWorker );
    } finally {
      deleteFile( filePath, "true");
      
      FileUtils.forceDelete( file );

      if ( knoxWasStopped && createFileOnNNWorker != null ) {
        assertTrue( KnoxUtils.startKnox( createFileOnNNWorker ) );
      }
    }
  }

  /**
   * Scenario is following:
   * 1) execute create file on NN request;
   * 2) start executing create medium file on DN request;
   * 3) find out what Knox instance is processing that request;
   * 4) shoot down that instance;
   * 5) verify that no fail over takes place.
   */
  @Test
  public void testNoFailoverDuringProcessingPutRequest() throws Exception {
    assertTrue( "All Knox instances must run", KnoxUtils.allKnoxesAreRunning() );

    String filePath = "/tmp/testNoFailoverDuringProcessingPutRequest.txt";
    final File file = new File( filePath );

    deleteFile( filePath, "true");

    RandomAccessFile randomAccessFile = new RandomAccessFile( file, "rw" );
    randomAccessFile.setLength( HDFS_FILE_SIZE );
    randomAccessFile.close();

    final String location = createFileNN( filePath, null, HttpStatus.SC_TEMPORARY_REDIRECT );
    assertNotNull( location );

    HAKnox createFileOnNNWorker = whoReceivedRequest( CREATE_FILE_ON_NN_REQUEST_IN_LOG_PATTERN );
    assertNotNull( createFileOnNNWorker );

    ExecutorService executor = Executors.newCachedThreadPool();

    Future<Integer> future = executor.submit( new Callable<Integer>() {
      public Integer call() throws Exception {
        return createFileDN( location, ContentType.DEFAULT_BINARY.getMimeType(), file, HttpStatus.SC_BAD_GATEWAY );
      }
    } );
    TimeUnit.SECONDS.sleep( 3 );

    boolean knoxWasStopped = KnoxUtils.stopKnox( createFileOnNNWorker );

    executor.shutdown();

    deleteFile( filePath, "true");

    FileUtils.forceDelete( file );

    if ( knoxWasStopped && createFileOnNNWorker != null ) {
      assertTrue( KnoxUtils.startKnox( createFileOnNNWorker ) );
    }

    int status = future.get();
    assertEquals( HttpStatus.SC_BAD_GATEWAY, status );
  }

  /**
   * Scenario is following:
   * 1) execute create file on NN and DN requests;
   * 2) start executing download medium file from DN request;
   * 3) find out what Knox instance is processing that request;
   * 4) shoot down that instance;
   * 5) verify that no fail over takes place.
   */
  @Test
  public void testNoFailoverDuringProcessingGetRequest() throws Exception {
    assertTrue( "All Knox instances must run", KnoxUtils.allKnoxesAreRunning() );

    final String filePath = "/tmp/testNoFailoverDuringProcessingGetRequest.txt";
    final File file = new File( filePath );

    deleteFile( filePath, "true");

    RandomAccessFile randomAccessFile = new RandomAccessFile( file, "rw" );
    randomAccessFile.setLength( HDFS_FILE_SIZE );
    randomAccessFile.close();

    final String location = createFileNN( filePath, null, HttpStatus.SC_TEMPORARY_REDIRECT );
    assertNotNull( location );

    HAKnox createFileOnNNWorker = whoReceivedRequest( CREATE_FILE_ON_NN_REQUEST_IN_LOG_PATTERN );
    assertNotNull( createFileOnNNWorker );

    createFileDN( location, ContentType.DEFAULT_BINARY.getMimeType(), file, HttpStatus.SC_CREATED );

    HAKnox createFileOnDNWorker = whoReceivedRequest( CREATE_FILE_ON_DN_REQUEST_IN_LOG_PATTERN );
    assertNotNull( createFileOnDNWorker );
    assertEquals( createFileOnNNWorker, createFileOnDNWorker );

    ExecutorService executor = Executors.newCachedThreadPool();

    Future<byte[]> future = executor.submit( new Callable<byte[]>() {
      public byte[] call() throws Exception {
        return downloadFile( filePath, HttpStatus.SC_OK );
      }
    } );
    TimeUnit.SECONDS.sleep( 3 );

    boolean knoxWasStopped = KnoxUtils.stopKnox( createFileOnNNWorker );

    executor.shutdown();

    deleteFile( filePath, "true");

    FileUtils.forceDelete( file );

    if ( knoxWasStopped && createFileOnNNWorker != null ) {
      assertTrue( KnoxUtils.startKnox( createFileOnNNWorker ) );
    }

    byte[] content = future.get();
    assertNotSame( HDFS_FILE_SIZE, content.length );
  }
}
