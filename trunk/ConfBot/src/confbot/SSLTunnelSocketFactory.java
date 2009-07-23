/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package confbot;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

/**
 *
 * @author Aman
 */
public class SSLTunnelSocketFactory extends SSLSocketFactory {

    private String tunnelHost;
    private int tunnelPort;
    private SSLSocketFactory dfactory;
    private String tunnelAuthUser, tunnelAuthPass;
    private Main main;
    
    public SSLTunnelSocketFactory(String proxyhost, String proxyport) {
        tunnelHost = proxyhost;
        tunnelPort = Integer.parseInt(proxyport);
        dfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
    
    public SSLTunnelSocketFactory(String proxyhost, String proxyport, String proxyUser, String proxyPass, Main main) {
        tunnelHost = proxyhost;
        tunnelPort = Integer.parseInt(proxyport);
        dfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        
        tunnelAuthUser = proxyUser;
        tunnelAuthPass = proxyPass;
        
        this.main = main;
    }

    
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(null, host.getHostName(), port, true);
    }

    public Socket createSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort) throws IOException {
        return createSocket(null, address.getHostName(), port, true);
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException, UnknownHostException {

        Socket tunnel = new Socket(tunnelHost, tunnelPort);

        doTunnelHandshake(tunnel, host, port);

        SSLSocket result = (SSLSocket) dfactory.createSocket(
                tunnel, host, port, autoClose);
        
        result.addHandshakeCompletedListener(
                new HandshakeCompletedListener() {

                    public void handshakeCompleted(HandshakeCompletedEvent event) {
                        main.debug_log("Handshake finished!");
                        main.debug_log(
                                "\t CipherSuite:" + event.getCipherSuite());
                        main.debug_log(
                                "\t SessionId " + event.getSession());
                        main.debug_log(
                                "\t PeerHost " + event.getSession().getPeerHost());
                    }
                });

        result.startHandshake();

        return result;
    }

    private void doTunnelHandshake(Socket tunnel, String host, int port)
            throws IOException {
        OutputStream out = tunnel.getOutputStream();
        /*
         * connector.append('Proxy-Connection: Keep-Alive')
        connector.append('Pragma: no-cache')
        connector.append('Host: %s:%s'%(self._hostIP,self._port))
        connector.append('User-Agent: Jabberpy/'+VERSION)
         */
        System.out.println(tunnelAuthUser+":"+tunnelAuthPass);
        String credentials = new sun.misc.BASE64Encoder().encode((tunnelAuthUser+":"+tunnelAuthPass).getBytes());
        String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n" + "Connection: keep-alive\nProxy-Connection: Keep-Alive\nPragma: no-cache\nUser-Agent: " + sun.net.www.protocol.http.HttpURLConnection.userAgent + "\nProxy-Authorization: Basic " + credentials + "\r\n\r\n";
        main.debug_log("Sending:\n" + msg + "--------\n");
        byte b[];
        try {
            /*
             * We really do want ASCII7 -- the http protocol doesn't change
             * with locale.
             */
            b = msg.getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            /*
             * If ASCII7 isn't there, something serious is wrong, but
             * Paranoia Is Good (tm)
             */
            b = msg.getBytes();
        }
        out.write(b);
        out.flush();

        /*
         * We need to store the reply so we can create a detailed
         * error message to the user.
         */
        byte reply[] = new byte[200];
        int replyLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false;     /* Done on first newline */

        InputStream in = tunnel.getInputStream();
        boolean error = false;

        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from proxy");
            }
            if (i == '\n') {
                headerDone = true;
                ++newlinesSeen;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone && replyLen < reply.length) {
                    reply[replyLen++] = (byte) i;
                }
            }
        }

        /*
         * Converting the byte array to a string is slightly wasteful
         * in the case where the connection was successful, but it's
         * insignificant compared to the network overhead.
         */
        String replyStr;
        try {
            replyStr = new String(reply, 0, replyLen, "ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            replyStr = new String(reply, 0, replyLen);
        }

        /* We check for Connection Established because our proxy returns 
         * HTTP/1.1 instead of 1.0 */
        //if (!replyStr.startsWith("HTTP/1.0 200")) {
        main.debug_log(replyStr);
        if (replyStr.toLowerCase().indexOf("200 connection established") == -1) {
            throw new IOException("Unable to tunnel through " + tunnelHost + ":" + tunnelPort + ".  Proxy returns \"" + replyStr + "\"");
        }

    /* tunneling Handshake was successful! */
    }

    public String[] getDefaultCipherSuites() {
        return dfactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return dfactory.getSupportedCipherSuites();
    }
}
