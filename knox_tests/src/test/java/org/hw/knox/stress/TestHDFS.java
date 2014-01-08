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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.hw.knox.KnoxKeys;
import org.hw.knox.NonHATestBase;
import org.hw.knox.Request;
import org.hw.knox.Response;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestHDFS extends NonHATestBase {

  private static final long HDFS_FILE_SIZE = 1024 * 1024 * 1024; // 1GB
  private static final String BASIC_DIRECTORY_PATH = "/tmp/KnoxStressTest/";

  @Before
  public void before() throws Exception {
    deleteFile( BASIC_DIRECTORY_PATH, "true", HttpStatus.SC_OK );
    createDir( BASIC_DIRECTORY_PATH, null, HttpStatus.SC_OK );
    File temporaryDitectory = new File( BASIC_DIRECTORY_PATH );
    FileUtils.deleteQuietly( temporaryDitectory );
    FileUtils.forceMkdir( temporaryDitectory );
  }

  @After
  public void after() throws Exception {
    deleteFile( BASIC_DIRECTORY_PATH, "true", HttpStatus.SC_OK );
    FileUtils.forceDelete( new File( BASIC_DIRECTORY_PATH ) );
  }

  @Test
  public void testLargeFileUploadAndDownload() throws Exception {
    String filePath = BASIC_DIRECTORY_PATH + "testLargeFileUploadAndDownload";
    executeFileUpload( filePath, HDFS_FILE_SIZE );
    executeConcurrentFileDownload( filePath, 1, HDFS_FILE_SIZE );
  }

  @Test
  public void testConcurrentFileUploadToHdfsWithLowPayload() throws Exception {
    int threadsNum = 100;
    executeConcurrentFileUploadToHdfs( "testConcurrentFileUploadToHdfsWithLowPayload", threadsNum, HDFS_FILE_SIZE / threadsNum );
  }

  @Test
  public void testConcurrentFileUploadToHdfsWithHighPayload() throws Exception {
    executeConcurrentFileUploadToHdfs( "testConcurrentFileUploadToHdfsWithHighPayload", 10, HDFS_FILE_SIZE );
  }

  @Test
  public void testConcurrentFileDownloadWithMediumPayload() throws Exception {
    int threadsNum = 100;
    String filePath = BASIC_DIRECTORY_PATH + "testConcurrentFileDownloadWithMediumSize";
    executeFileUpload( filePath, HDFS_FILE_SIZE / threadsNum );
    executeConcurrentFileDownload(filePath, threadsNum, HDFS_FILE_SIZE / threadsNum );
  }

  @Test
  public void testConcurrentFileDownloadWithLowPayload() throws Exception {
    int threadsNum = 100;
    String filePath = BASIC_DIRECTORY_PATH + "testConcurrentFileDownloadWithSmallSize";
    executeFileUpload( filePath, HDFS_FILE_SIZE / threadsNum );
    executeConcurrentFileDownload(filePath, threadsNum, HDFS_FILE_SIZE / threadsNum);
  }

  private void executeConcurrentFileUploadToHdfs( String testDir, int threadsNum, long fileSize ) throws Exception {
    String directoryPath = BASIC_DIRECTORY_PATH + testDir;
    FileUtils.forceMkdir( new File( directoryPath ) );

    long totalFilesSize = threadsNum * fileSize;
    final AtomicLong transferredDataSize = new AtomicLong();

    List<File> files = new ArrayList<File>( threadsNum );

    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      for ( int i = 0; i < threadsNum; i++ ) {
        File file = new File( directoryPath, "file_" + i );
        files.add( file );

        RandomAccessFile randomAccessFile = new RandomAccessFile( file, "rw" );
        randomAccessFile.setLength( fileSize );
        randomAccessFile.close();
      }

      List<Callable<Void>> callables = new ArrayList<Callable<Void>>( threadsNum );
      for ( final File file : files ) {
        callables.add( new Callable<Void>() {
          public Void call() throws Exception {
            createFile( file.getAbsolutePath(), null, ContentType.DEFAULT_BINARY.getMimeType(), file, HttpStatus.SC_TEMPORARY_REDIRECT, HttpStatus.SC_CREATED, HttpStatus.SC_OK );

            Request request = new Request();
            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put( "op", "GETFILESTATUS" );
            request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + file.getAbsolutePath() );
            request.setQueryParams( queryParams );
            Response response = session.get( request );
            assertEquals( HttpStatus.SC_OK, response.getStatusCode() );

            transferredDataSize.addAndGet( new JSONObject( new String( response.getContent() ) ).getJSONObject( "FileStatus" ).getLong( "length" ) );

            return null;
          }
        } );
      }

      deleteFile( directoryPath, "true", HttpStatus.SC_OK );
      createDir( directoryPath, null, HttpStatus.SC_OK );

      executor.invokeAll( callables );
    } finally {
      for ( File file : files ) {
        FileUtils.forceDelete( file );
      }

      executor.shutdown();

      deleteFile( directoryPath, "true", HttpStatus.SC_OK );

      assertEquals( totalFilesSize, transferredDataSize.get() );
    }
  }

  private void executeFileUpload( String filePath, long fileSize ) throws Exception {
    File file = new File( filePath );
    RandomAccessFile randomAccessFile = new RandomAccessFile( file, "rw" );
    randomAccessFile.setLength( fileSize );
    randomAccessFile.close();
    createFile( filePath, null, ContentType.DEFAULT_BINARY.getMimeType(), file, HttpStatus.SC_TEMPORARY_REDIRECT, HttpStatus.SC_CREATED, HttpStatus.SC_OK );

    Request request = new Request();
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put( "op", "GETFILESTATUS" );
    request.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + filePath );
    request.setQueryParams( queryParams );
    Response response = session.get( request );
    assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
    assertEquals( fileSize, new JSONObject( new String( response.getContent() ) ).getJSONObject( "FileStatus" ).getLong( "length" ) );
  }

  private void executeConcurrentFileDownload( final String filePath, int threadsNum, final long fileSize ) throws Exception {
    ExecutorService executor = Executors.newCachedThreadPool();
    final AtomicLong transferredDataSize = new AtomicLong();
    List<Callable<Void>> callables = new ArrayList<Callable<Void>>( threadsNum );
    long totalFilesSize = fileSize * threadsNum;
    try {
      
      for ( int i = 0; i < threadsNum; i++ ) {
        callables.add( new Callable<Void>() {
          public Void call() throws Exception {
            Request readRequest = new Request();
            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put( "op", "OPEN" );
            readRequest.setPath( baseUrl + KnoxKeys.NAMENODE_PATH + filePath );
            readRequest.setQueryParams( queryParams );
            
            Response response = session.get( readRequest );
            
            assertEquals( HttpStatus.SC_OK, response.getStatusCode() );
            transferredDataSize.addAndGet( response.getContent().length );
            
            return null;
          }
        } );
        
      }
      executor.invokeAll( callables );
    } finally {
      executor.shutdown();
      assertEquals( totalFilesSize, transferredDataSize.get() );
    }
  }

}
