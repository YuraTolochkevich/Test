/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.hw.knox.util.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;

public class CMFMasterService {

  private static final Logger LOG = Logger.getLogger( AESEncryptor.class );
  private static final String MASTER_PASSPHRASE = "masterpassphrase";
  private static final String MASTER_PERSISTENCE_TAG = "#1.0# " + TimeStamp.getCurrentTime().toDateString();
  protected char[] master = null;
  protected String serviceName = "gateway";
  private AESEncryptor aes = new AESEncryptor( MASTER_PASSPHRASE );

  public CMFMasterService() {
  }

  public void setMasterSecret( char[] master ) {
    this.master = master;
  }

  public char[] getMasterSecret() {
    return this.master;
  }

  public void setupMasterSecret( String securityDir, String filename ) throws Exception {
    File masterFile = new File( securityDir, filename );
    if ( masterFile.exists() ) {
      initializeFromMaster( masterFile );
    } else {
      persistMaster( master, masterFile );
    }
  }

  protected void persistMaster( char[] master, File masterFile ) {
    EncryptionResult atom = encryptMaster( master );
    try {
      ArrayList<String> lines = new ArrayList<String>();
      lines.add( MASTER_PERSISTENCE_TAG );

      String line = Base64.encodeBase64String( ( Base64.encodeBase64String( atom.salt ) + "::"
          + Base64.encodeBase64String( atom.iv ) + "::"
          + Base64.encodeBase64String( atom.cipher ) ).getBytes( "UTF8" ) );
      lines.add( line );
      FileUtils.writeLines( masterFile, "UTF8", lines );

      // restrict os permissions to only the user running this process
      chmod( "600", masterFile );
    } catch ( IOException e ) {
      LOG.error( e.getMessage(), e.fillInStackTrace() );
    }
  }

  private EncryptionResult encryptMaster( char[] master ) {
    // TODO Auto-generated method stub
    try {
      return aes.encrypt( new String( master ) );
    } catch ( Exception e ) {
      LOG.error( e.getMessage(), e.fillInStackTrace() );
    }
    return null;
  }

  protected void initializeFromMaster( File masterFile ) throws Exception {
    List<String> lines = FileUtils.readLines( masterFile, "UTF8" );
    String tag = lines.get( 0 );
    String line = new String( Base64.decodeBase64( lines.get( 1 ) ) );
    String[] parts = line.split( "::" );
    this.master = new String( aes.decrypt( Base64.decodeBase64( parts[0] ), Base64.decodeBase64( parts[1] ), Base64.decodeBase64( parts[2] ) ), "UTF8" ).toCharArray();
  }

  private void chmod( String args, File file ) throws IOException {
    // TODO: move to Java 7 NIO support to add windows as well
    // TODO: look into the following for Windows: Runtime.getRuntime().exec("attrib -r myFile");
    if ( isUnixEnv() ) {
      //args and file should never be null.
      if ( args == null || file == null ) {
        throw new IllegalArgumentException( "nullArg" );
      }
      if ( !file.exists() ) {
        throw new IOException( "fileNotFound" );
      }

      // " +" regular expression for 1 or more spaces
      final String[] argsString = args.split( " +" );
      List<String> cmdList = new ArrayList<String>();
      cmdList.add( "/bin/chmod" );
      cmdList.addAll( Arrays.asList( argsString ) );
      cmdList.add( file.getAbsolutePath() );
      new ProcessBuilder( cmdList ).start();
    }
  }

  private boolean isUnixEnv() {
    return ( File.separatorChar == '/' );
  }
}