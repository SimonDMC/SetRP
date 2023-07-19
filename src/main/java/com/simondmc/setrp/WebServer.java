package com.simondmc.setrp;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {
    private static HttpServer server;
    public static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(26668), 0);
        server.createContext("/", new StaticFileServer());
        server.setExecutor(null);
        server.start();
    }
    public static void stop() {
        server.stop(0);
    }
}
