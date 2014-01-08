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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;

public class Session {

  private String base;
  private HttpHost host;
  private DefaultHttpClient client;
  private BasicHttpContext context;

  public static Session login( String url, String username, String password ) throws Exception {
    return new Session( url, username, password );
  }

  private Session( String url, String username, String password ) throws Exception {
    this.base = url;

    URI uri = new URI( this.base );
    host = new HttpHost( uri.getHost(), uri.getPort(), uri.getScheme() );

    try {
      client = createClient();
      client.getCredentialsProvider().setCredentials(
          new AuthScope( host.getHostName(), host.getPort() ),
          new UsernamePasswordCredentials( username, password ) );
      AuthCache authCache = new BasicAuthCache();
      BasicScheme authScheme = new BasicScheme();
      authCache.put( host, authScheme );
      context = new BasicHttpContext();
      context.setAttribute( ClientContext.AUTH_CACHE, authCache );
    } catch ( GeneralSecurityException e ) {
      throw new Exception( "Failed to create HTTP client.", e );
    }
  }

  public void close() {
    if ( client != null ) {
      client.getConnectionManager().shutdown();
    }
  }

  private static DefaultHttpClient createClient() throws GeneralSecurityException {
    SchemeRegistry registry = new SchemeRegistry();
    SSLSocketFactory socketFactory = new SSLSocketFactory( new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER );
    registry.register( new Scheme( "https", 443, socketFactory ) );
    registry.register( new Scheme( "http", 80, new PlainSocketFactory() ) );
    PoolingClientConnectionManager mgr = new PoolingClientConnectionManager( registry );
    mgr.setDefaultMaxPerRoute( Integer.MAX_VALUE );
    mgr.setMaxTotal( Integer.MAX_VALUE );
    DefaultHttpClient client = new DefaultHttpClient( mgr, new DefaultHttpClient().getParams() );
    return client;
  }

  public Response put( Request request ) throws Exception {
    URIBuilder uri = new URIBuilder( request.getPath() );
    if ( request.getQueryParams() != null ) {
      for ( Map.Entry<String, String> queryParam : request.getQueryParams().entrySet() ) {
        uri.addParameter( queryParam.getKey(), queryParam.getValue() );
      }
    }
    HttpPut put = new HttpPut( uri.build() );
    HttpEntity entity = null;
    if ( request.getContentBytes() != null ) {
      entity = new ByteArrayEntity( request.getContentBytes(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getContentString() != null ) {
      entity = new StringEntity( request.getContentString(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getContentFile() != null ) {
      entity = new FileEntity( request.getContentFile(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getFormParams() != null && !request.getFormParams().isEmpty() ) {
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      for ( Map.Entry<String, String> formParam : request.getFormParams().entries()) {
        params.add( new BasicNameValuePair( formParam.getKey(), formParam.getValue() ) );
      }
      entity = new UrlEncodedFormEntity( params );
    }
    if ( entity != null ) {
      put.setEntity( entity );
    }
    if ( request.getAccept() != null ) {
      put.setHeader( "Accept", request.getAccept() );
    }
    HttpResponse httpResponse = client.execute( host, put, context );
    Response response = new Response();
    response.setStatusCode( httpResponse.getStatusLine().getStatusCode() );

    for ( Header header : httpResponse.getAllHeaders() ) {
      response.addHeader( header.getName(), header.getValue() );
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy( httpResponse.getEntity().getContent(), baos );
    response.setContent( baos.toByteArray() );

    return response;
  }

  public Response post( Request request ) throws Exception {
    URIBuilder uri = new URIBuilder( request.getPath() );
    if ( request.getQueryParams() != null ) {
      for ( Map.Entry<String, String> queryParam : request.getQueryParams().entrySet() ) {
        uri.addParameter( queryParam.getKey(), queryParam.getValue() );
      }
    }
    HttpPost post = new HttpPost( uri.build() );
    HttpEntity entity = null;
    if ( request.getContentBytes() != null ) {
      entity = new ByteArrayEntity( request.getContentBytes(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getContentString() != null ) {
      entity = new StringEntity( request.getContentString(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getContentFile() != null ) {
      entity = new FileEntity( request.getContentFile(), ContentType.create( request.getContentType(), "UTF-8" ) );
    } else if ( request.getFormParams() != null && !request.getFormParams().isEmpty() ) {
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      for ( Map.Entry<String, String> formParam : request.getFormParams().entries()) {
        params.add( new BasicNameValuePair( formParam.getKey(), formParam.getValue() ) );
      }
      entity = new UrlEncodedFormEntity( params );
    }
    if ( entity != null ) {
      post.setEntity( entity );
    }
    if ( request.getAccept() != null ) {
      post.setHeader( "Accept", request.getAccept() );
    }
    HttpResponse httpResponse = client.execute( host, post, context );
    Response response = new Response();
    response.setStatusCode( httpResponse.getStatusLine().getStatusCode() );

    for ( Header header : httpResponse.getAllHeaders() ) {
      response.addHeader( header.getName(), header.getValue() );
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy( httpResponse.getEntity().getContent(), baos );
    response.setContent( baos.toByteArray() );

    return response;
  }

  public Response get( Request request ) throws Exception {
    URIBuilder uri = new URIBuilder( request.getPath() );
    if ( request.getQueryParams() != null ) {
      for ( Map.Entry<String, String> queryParam : request.getQueryParams().entrySet() ) {
        uri.addParameter( queryParam.getKey(), queryParam.getValue() );
      }
    }
    HttpGet get = new HttpGet( uri.build() );
    if ( request.getAccept() != null ) {
      get.setHeader( "Accept", request.getAccept() );
    }
    HttpResponse httpResponse = client.execute( host, get, context );
    Response response = new Response();
    response.setStatusCode( httpResponse.getStatusLine().getStatusCode() );

    for ( Header header : httpResponse.getAllHeaders() ) {
      response.addHeader( header.getName(), header.getValue() );
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy( httpResponse.getEntity().getContent(), baos );
    response.setContent( baos.toByteArray() );

    return response;
  }

  public Response delete( Request request ) throws Exception {
    URIBuilder uri = new URIBuilder( request.getPath() );
    if ( request.getQueryParams() != null ) {
      for ( Map.Entry<String, String> queryParam : request.getQueryParams().entrySet() ) {
        uri.addParameter( queryParam.getKey(), queryParam.getValue() );
      }
    }
    HttpDelete delete = new HttpDelete( uri.build() );
    if ( request.getAccept() != null ) {
      delete.setHeader( "Accept", request.getAccept() );
    }
    HttpResponse httpResponse = client.execute( host, delete, context );
    Response response = new Response();
    response.setStatusCode( httpResponse.getStatusLine().getStatusCode() );

    for ( Header header : httpResponse.getAllHeaders() ) {
      response.addHeader( header.getName(), header.getValue() );
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy( httpResponse.getEntity().getContent(), baos );
    response.setContent( baos.toByteArray() );

    return response;
  }
}
