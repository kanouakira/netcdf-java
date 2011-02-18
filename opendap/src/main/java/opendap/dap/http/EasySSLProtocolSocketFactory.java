/*
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package opendap.dap.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.ssl.*;

/**
 * <p/>
 * EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s
 * that accept self-signed certificates.
 * </p>
 * <p/>
 * This socket factory SHOULD NOT be used for productive systems
 * due to security reasons, unless it is a concious decision and
 * you are perfectly aware of security implications of accepting
 * self-signed certificates
 * </p>
 * <p/>
 * <p/>
 * Example of using custom protocol socket factory for a specific host:
 * <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 * <p/>
 *     HttpClient client = new HttpClient();
 *     client.getHostConfiguration().setHost("localhost", 443, easyhttps);
 *     // use relative url only
 *     GetMethod httpget = new GetMethod("/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 * <p/>
 * Example of using custom protocol socket factory per default instead of the standard one:
 * <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 *     Protocol.registerProtocol("https", easyhttps);
 * <p/>
 *     HttpClient client = new HttpClient();
 *     GetMethod httpget = new GetMethod("https://localhost/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 *
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 *         <p/>
 *         <p/>
 *         DISCLAIMER: HttpClient developers DO NOT actively support this component.
 *         The component is provided as a reference material, which may be inappropriate
 *         for use without additional customization.
 *         </p>
 */

public class EasySSLProtocolSocketFactory implements ProtocolSocketFactory
{

/**
 * Log object for this class.
 */
/* FIX
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EasySSLProtocolSocketFactory.class);
//  private static final Log LOG = LogFactory.getLog(EasySSLProtocolSocketFactory.class);
*/

private SSLContext sslcontext = null;

/**
 * Constructor for EasySSLProtocolSocketFactory.
 */
public EasySSLProtocolSocketFactory()
{
    super();
}


/**
 * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
 */
public Socket createSocket(
        String host,
        int port,
        InetAddress clientHost,
        int clientPort)
        throws IOException, UnknownHostException
{

    return getSSLContext().getSocketFactory().createSocket(
            host,
            port,
            clientHost,
            clientPort
                                                          );
}

/**
 * Attempts to get a new socket connection to the given host within the given time limit.
 * <p/>
 * To circumvent the limitations of older JREs that do not support connect timeout a
 * controller thread is executed. The controller thread attempts to create a new socket
 * within the given limit of time. If socket constructor does not return until the
 * timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
 * </p>
 *
 * @param host         the host name/IP
 * @param port         the port on the host
 * @param localAddress the local host name/IP to bind the socket to
 * @param localPort    the port on the local machine
 * @param params       {@link HttpConnectionParams Http connection parameters}
 * @return Socket a new socket
 * @throws IOException          if an I/O error occurs while creating the socket
 * @throws UnknownHostException if the IP address of the host cannot be
 *                              determined
 */
public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params
                          ) throws IOException, UnknownHostException, ConnectTimeoutException
{
    if (params == null) {
        throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    if (timeout == 0) {
        return createSocket(host, port, localAddress, localPort);
    } else {
        // To be eventually deprecated when migrated to Java 1.4 or above
        return ControllerThreadSocketFactory.createSocket(
                this, host, port, localAddress, localPort, timeout);
    }
}

/**
 * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int)
 */
public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
{
    return getSSLContext().getSocketFactory().createSocket(
            host,
            port
                                                          );
}

/**
 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
 */
public Socket createSocket(
        Socket socket,
        String host,
        int port,
        boolean autoClose)
        throws IOException, UnknownHostException
{
    return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
                                                          );
}

/**
 * Add code to handle ESG authorization using a keystore
 * H/T to Apache and Philip Kershaw and Jon Blower for this code.
 */
private static KeyStore createKeyStore(final File keystorefile, final String password)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
{
    KeyStore keystore = KeyStore.getInstance("jks");
    InputStream is = null;
    try {
        is = new FileInputStream(keystorefile);
        keystore.load(is, password.toCharArray());
    } finally {
        if (is != null) is.close();
    }
    return keystore;
}

private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException
{
    KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
    kmfactory.init(keystore, password != null ? password.toCharArray() : null);
    return kmfactory.getKeyManagers();
}

private static TrustManager[] createTrustManagers(final KeyStore keystore)
        throws KeyStoreException, NoSuchAlgorithmException
{
    return new TrustManager[]{new EasyX509TrustManager(keystore)};
}

private SSLContext createSSLContext()  throws HTTPException
{
    boolean orig = true;
    boolean esg = true;
    try {
        KeyManager[] keymanagers = null;
        KeyStore keystore = null;
        TrustManager[] trustmanagers = null;
        String keystorepath = null;
        String keystorepassword = null;

        if(!orig) {
            keystorepath = System.getProperty("keystore");
            keystorepassword = System.getProperty("keystorepassword");
        }
        if(keystorepassword != null) {
            keystorepassword = keystorepassword.trim();
            if(keystorepassword.length() == 0) keystorepassword = null;
        }

        if(keystorepath != null && keystorepassword != null) {
            File keystorefile = new File(keystorepath);
            if(!keystorefile.canRead())
                throw new HTTPException("Cannot read specified keystore:"+keystorefile.getAbsolutePath());
            keystore = KeyStore.getInstance("JKS");
            InputStream is = null;
            try {
                is = new FileInputStream(keystorefile);
                keystore.load(is, keystorepassword.toCharArray());
            } finally {
                if (is != null) is.close();
            }
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
            kmfactory.init(keystore, keystorepassword.toCharArray());
            keymanagers = kmfactory.getKeyManagers();
        }
        trustmanagers = new TrustManager[]{new EasyX509TrustManager(keystore)};

        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(keymanagers, trustmanagers, null);
        return sslcontext;

    } catch (NoSuchAlgorithmException e) {
        throw new HTTPException("Unsupported algorithm exception: " + e.getMessage());
    } catch (KeyStoreException e) {
        throw new HTTPException("Keystore exception: " + e.getMessage());
    } catch (GeneralSecurityException e) {
        throw new HTTPException("Key management exception: " + e.getMessage());
    } catch (IOException e) {
        throw new HTTPException("I/O error reading keystore/truststore file: " + e.getMessage());
    }
}

private SSLContext getSSLContext() throws HTTPException
{
    if (this.sslcontext == null) {
        this.sslcontext = createSSLContext();
    }
    return this.sslcontext;
}

}