package uk.ac.ed.inf;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

public class MockRestServer {
    HttpServer httpServer;
    public MockRestServer(int port) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    }

    public HttpContext getContextToServeDataOnUrl(String url, String data) {
         return httpServer.createContext(url, new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = data.getBytes();
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });
    }
    public void removeContext(HttpContext context) {
        httpServer.removeContext(context);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}
