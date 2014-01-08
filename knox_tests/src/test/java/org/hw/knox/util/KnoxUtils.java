/*
 * Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.
 *
 *
 * Except as expressly permitted in a written Agreement between your
 * company and Hortonworks, Inc, any use, reproduction, modification,
 * redistribution or other exploitation of all or any part of the contents
 * of this file is strictly prohibited.
 */
package org.hw.knox.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hw.knox.HAKnox;
import org.hw.knox.KnoxKeys;
import org.hw.knox.util.security.CMFMasterService;

public class KnoxUtils {
  
  private static final Logger LOG = Logger.getLogger( KnoxUtils.class );
  private static final String KNOX_IS_RUNNING_STRING = "Knox is running";
  private static final String KNOX_IS_STARTED_REGEX = "Starting Knox succeeded with PID=[0-9]*\\.";
  private static final String KNOX_IS_STOPPED_REGEX = "Stopping Knox \\[[0-9]*\\] succeeded\\.";

  private static String getSHFileRelatedPath( HAKnox knox ) {
    return "." + File.separator
        + KnoxKeys.KNOX_BIN_DIR_PATH + File.separator
        + KnoxKeys.KNOX_SH_FILE_NAME;
  }

  public static String getKnoxStatus( HAKnox knox ) throws IOException, InterruptedException {
    String shellCommand[] = new String[] {
      "sh",
      "-c",
      getSHFileRelatedPath( knox ) + " status"
    };

    Process process = Runtime.getRuntime().exec( shellCommand, new String[] {}, new File( knox.homeDir ) );
    BufferedReader inReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
    process.waitFor();

    return inReader.readLine();
  }

  public static boolean knoxIsRunning( HAKnox knox ) throws IOException, InterruptedException {
    String status = getKnoxStatus( knox );
    return status != null && status.contains( KNOX_IS_RUNNING_STRING );
  }

  public static boolean allKnoxesAreRunning() throws IOException, InterruptedException {
    boolean result = true;
    for ( HAKnox haKnox : HAKnox.values() ) {
      result = result && knoxIsRunning( haKnox );
    }
    return result;
  }

  public static void setupKnoxMasterSecret( CMFMasterService service, HAKnox haKnox ) throws Exception {
    service.setMasterSecret( KnoxKeys.KNOX_MASTER_SECRET.toCharArray() );

    String securityDir = haKnox.homeDir
        + ( haKnox.homeDir.endsWith( File.separator ) ? "" : File.separator )
        + KnoxKeys.KNOX_SECURITY_DIR_REL_PATH;
    String filename = KnoxKeys.KNOX_MASTER_SECRET_FILE_NAME;
    service.setupMasterSecret( securityDir, filename );
  }

  public static void setupAllKnoxesMasterSecret( CMFMasterService service ) throws Exception {
    for ( HAKnox haKnox : HAKnox.values() ) {
      setupKnoxMasterSecret( service, haKnox );
    }
  }

  public static boolean setupKnox( HAKnox knox ) throws IOException, InterruptedException {
    String shellCommand[] = new String[] {
      "sh",
      "-c",
      getSHFileRelatedPath( knox ) + " setup"
    };

    Process process = Runtime.getRuntime().exec( shellCommand, new String[] {}, new File( knox.homeDir ) );
    process.waitFor();

    return true;
  }

  public static void setupAllKnoxes() throws IOException, InterruptedException {
    for ( HAKnox haKnox : HAKnox.values() ) {
      setupKnox( haKnox );
    }
  }

  public static boolean startKnox( HAKnox knox ) throws IOException, InterruptedException {
    String shellCommand[] = new String[] {
      "sh",
      "-c",
      getSHFileRelatedPath( knox ) + " start"
    };

    Process process = Runtime.getRuntime().exec( shellCommand, new String[] {}, new File( knox.homeDir ) );
    BufferedReader inReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
    process.waitFor();
     String line;
    while ( ( line = inReader.readLine() ) != null ) {
      if ( Pattern.matches( KNOX_IS_STARTED_REGEX, line ) ) {
        return true;
      }
    }

    return false;
  }

  public static void startAllKnoxes() throws IOException, InterruptedException {
    for ( HAKnox haKnox : HAKnox.values() ) {
      startKnox( haKnox );
    }
  }

  public static boolean stopKnox( HAKnox knox ) throws IOException, InterruptedException {
    String shellCommand[] = new String[] {
      "sh",
      "-c",
      getSHFileRelatedPath( knox ) + " stop"
    };

    Process process = Runtime.getRuntime().exec( shellCommand, new String[] {}, new File( knox.homeDir ) );
    BufferedReader inReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
    process.waitFor();

    String line;
    while ( ( line = inReader.readLine() ) != null ) {
      if ( Pattern.matches( KNOX_IS_STOPPED_REGEX, line ) ) {
        return true;
      }
    }

    return false;
  }

  public static void stopAllKnoxes() throws IOException, InterruptedException {
    for ( HAKnox haKnox : HAKnox.values() ) {
      stopKnox( haKnox );
    }
  }

  public static void clearAllKnoxLogs() throws IOException {
    for ( HAKnox haKnox : HAKnox.values() ) {
      clearKnoxLog( haKnox );
    }
  }

  private static String getLogFilePath( HAKnox knox ) {
    return knox.logDir
        + ( knox.logDir.endsWith( File.separator ) ? "" : File.separator )
        + KnoxKeys.KNOX_LOG_FILE_NAME;
  }

  public static void clearKnoxLog( HAKnox knox ) throws IOException {
    String logFilePath = getLogFilePath( knox );
    FileOutputStream fileOutputStream = null;
    IOException cause = null;
    try {
      fileOutputStream = new FileOutputStream( logFilePath );
      fileOutputStream.write( "".getBytes() );
    } catch ( IOException ioe ) {
      cause = ioe;
    } finally {
      if ( fileOutputStream != null ) {
        try {
          fileOutputStream.close();
        } catch ( IOException ioe ) {
          LOG.error( ioe.getMessage(), ioe.fillInStackTrace() );
        }
      }
      if ( cause != null ) {
        throw cause;
      }
    }
  }

  public static boolean knoxReceivedRequest( HAKnox knox, String requestPattern ) throws IOException {
    String logFilePath = getLogFilePath( knox );
    BufferedReader bufferedReader = null;
    IOException cause = null;
    try {
      bufferedReader = new BufferedReader( new FileReader( logFilePath ) );
      String line;
      while ( ( line = bufferedReader.readLine() ) != null ) {
        if ( Pattern.matches( requestPattern, line ) ) {
          return true;
        }
      }
    } catch ( IOException ioe ) {
      cause = ioe;
    } finally {
      if ( bufferedReader != null ) {
        try {
          bufferedReader.close();
        } catch ( IOException ioe ) {
          LOG.error( ioe.getMessage(), ioe.fillInStackTrace() );
        }
      }
      if ( cause != null ) {
        throw cause;
      }
    }
    return false;
  }

  public static void synchKeystores( HAKnox source ) throws IOException {
    String keystoresSourcePath = source.homeDir
        + ( source.homeDir.endsWith( File.separator ) ? "" : File.separator )
        + KnoxKeys.KNOX_KEYSTORES_DIR_REL_PATH;
    File keystoresSourceDir = new File( keystoresSourcePath );
    for ( HAKnox haKnox : HAKnox.values() ) {
      if ( haKnox != source ) {
        String keystoresDestinationPath = haKnox.homeDir
            + ( haKnox.homeDir.endsWith( File.separator ) ? "" : File.separator )
            + KnoxKeys.KNOX_KEYSTORES_DIR_REL_PATH;
        File keystoresDestinationDir = new File( keystoresDestinationPath );

        FileUtils.deleteQuietly( new File( keystoresDestinationPath ) );
        FileUtils.copyDirectory( keystoresSourceDir, keystoresDestinationDir);
      }
    }
  }
}
