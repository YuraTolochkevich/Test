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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class KnoxKeys {

  // load the properties file and read the data in
  public static final Properties properties = loadProperties( "knox.properties" );
  public static final String LOG4J_FILE = properties.getProperty( "LOG4J_FILE" );
  public static final String LOCAL_DATA_DIR = properties.getProperty( "LOCAL_DATA_DIR" );
  //////// Knox data
  public static final String KNOX_SCHEME = properties.getProperty( "KNOX_SCHEME" );
  public static final String KNOX_HOST = properties.getProperty( "KNOX_HOST" );
  public static final String KNOX_PORT = properties.getProperty( "KNOX_PORT" );
  public static final String KNOX_CONTEXT = properties.getProperty( "KNOX_CONTEXT" );
  public static final String KNOX_LOG_FILE_NAME = "knox.out";
  public static final String KNOX_BIN_DIR_PATH = "bin";
  public static final String KNOX_SH_FILE_NAME = "gateway.sh";
  //////// begin of HA section
  public static final String KNOX_A_LOG_DIR = properties.getProperty( "KNOX_A_LOG_DIR" );
  public static final String KNOX_A_HOME_DIR = properties.getProperty( "KNOX_A_HOME" );
  public static final String KNOX_B_LOG_DIR = properties.getProperty( "KNOX_B_LOG_DIR" );
  public static final String KNOX_B_HOME_DIR = properties.getProperty( "KNOX_B_HOME" );
  public static final String HA_SCHEME = properties.getProperty( "HA_SCHEME" );
  public static final String HA_HOST = properties.getProperty( "HA_HOST" );
  public static final String HA_PORT = properties.getProperty( "HA_PORT" );
  public static final String HA_CONTEXT = properties.getProperty( "HA_CONTEXT" );
  public static final String KNOX_SECURITY_DIR_REL_PATH = properties.getProperty( "KNOX_SECURITY_DIR_REL_PATH" );
  public static final String KNOX_KEYSTORES_DIR_REL_PATH = properties.getProperty( "KNOX_KEYSTORES_DIR_REL_PATH" );
  public static final String KNOX_MASTER_SECRET = properties.getProperty( "KNOX_MASTER_SECRET" );
  public static final String KNOX_MASTER_SECRET_FILE_NAME = properties.getProperty( "KNOX_MASTER_SECRET_FILE_NAME" );
  //////// end of HA section
  public static final String USER_NAME = properties.getProperty( "USER_NAME" );
  public static final String USER_PASSWORD = properties.getProperty( "USER_PASSWORD" );
  public static final String NAMENODE_PATH = properties.getProperty( "NAMENODE_PATH" );
  public static final String DATANODE_PATH = properties.getProperty( "DATANODE_PATH" );
  public static final String TEMPLETON_PATH = properties.getProperty( "TEMPLETON_PATH" );
  public static final String HBASE_PATH = properties.getProperty( "HBASE_PATH" );
  // get the version on hadoop defaults to 2.0
  public static final String HADOOP_VERSION = properties.getProperty( "HADOOP_VERSION", "2.0" );
  // get the property that determines the security
  public static final String HADOOP_SECURITY_PROPERTY = properties.getProperty( "HADOOP_SECURITY_PROPERTY" );
  private static final Logger LOG = Logger.getLogger( KnoxKeys.class );

  private static Properties loadProperties( String file ) {
    Properties result = null;
    try {
      FileReader fr = new FileReader( file );
      result = new Properties();
      result.load( fr );
    } catch ( FileNotFoundException e ) {
      LOG.error( e.getMessage(), e.fillInStackTrace() );
    } catch ( IOException e ) {
      LOG.error( e.getMessage(), e.fillInStackTrace() );
    }

    return result;
  }
}
