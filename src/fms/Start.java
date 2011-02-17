package fms;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Locale;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

public class Start {
	
	private static final String DOCROOT = "www";

    public static void main(String[] args) throws Exception {
        int port = 80;
        if(args.length!=0)
          port = Integer.parseInt(args[0]);
        Thread t = new RequestListenerThread(port, DOCROOT);
        t.setDaemon(false);
        t.start();
    }
    
  
    
    static class RequestListenerThread extends Thread {

        private final ServerSocket serversocket;
        private final HttpParams params; 
        private final HttpService httpService;
        
        public RequestListenerThread(int port, final String docroot) throws IOException {
            this.serversocket = new ServerSocket(port);
            this.params = new SyncBasicHttpParams();
            
            HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
                    new ResponseDate(),
                    new ResponseServer(),
                    new ResponseContent(),
                    new ResponseConnControl()
            });
            
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("/fms*", new FMS());
            reqistry.register("*", new HttpFileHandler(docroot));
            
            // Set up the HTTP service
            this.httpService = new HttpService(
                    httpproc, 
                    new DefaultConnectionReuseStrategy(), 
                    new DefaultHttpResponseFactory(),
                    reqistry,
                    this.params);
        }
        
        public void run() {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, this.params);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn);
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error initialising connection thread: " 
                            + e.getMessage());
                    break;
                }
            }
        }
    }
    
    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;
        
        public WorkerThread(
                final HttpService httpservice, 
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }
        
        public void run() {
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    }
    /**
     * Handles request for files in docroot
     * @author mike
     *
     */
    static class HttpFileHandler implements HttpRequestHandler  {
        
        private final String docRoot;
        
        public HttpFileHandler(final String docRoot) {
            super();
            this.docRoot = docRoot;
        }
        
        public void handle(
                final HttpRequest request, 
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported"); 
            }
            String target = request.getRequestLine().getUri();
            
            //URL rewriting
            if(target.equals("/")) target = "index";
            if(target.indexOf('?')!=-1)
                target = target.substring(0, target.indexOf('?'));
            
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);
                System.out.println("Incoming entity content (bytes): " + entityContent.length);
            }
            
            final File file = new File(this.docRoot, URLDecoder.decode(target, "UTF-8"));
            if (!file.exists()) {

                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                EntityTemplate body = new EntityTemplate(new ContentProducer() {
                    
                    public void writeTo(final OutputStream outstream) throws IOException {
                        OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                        writer.write("<html><body><h1>");
                        writer.write("File ");
                        writer.write(file.getPath());
                        writer.write(" not found");
                        writer.write("</h1></body></html>");
                        writer.flush();
                    }
                    
                });
                body.setContentType("text/html; charset=UTF-8");
                response.setEntity(body);
                System.out.println("File " + file.getPath() + " not found");
                
            } else {
                
                response.setStatusCode(HttpStatus.SC_OK);
                String contentType = target.endsWith("js") ? "text/javascript" : target.endsWith("css") ? "text/css" : "text/html";
               
                FileEntity body = new FileEntity(file, contentType);
                response.setEntity(body);
                System.out.println("Serving file " + file.getPath());
                
            }
        }
        
    }
}
