package com.simondmc.setrp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileServer implements HttpHandler {
    @Override
    public void handle(HttpExchange he) throws IOException {
        File file = new File(SetRP.plugin.getDataFolder() + "/rp.zip");
        he.sendResponseHeaders(200, file.length());
        OutputStream os = he.getResponseBody();
        Files.copy(file.toPath(), os);
        os.close();
    }
}
