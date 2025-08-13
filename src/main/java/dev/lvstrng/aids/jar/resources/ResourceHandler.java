package dev.lvstrng.aids.jar.resources;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.jar.resources.impl.ManifestHandler;
import dev.lvstrng.aids.utils.ZipUtils;

import java.io.IOException;
import java.util.jar.Manifest;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("all")
public class ResourceHandler {
    public void handle(ZipOutputStream zos) {
        for(var resource : Jar.getResources().entrySet()) {
            var name =  resource.getKey();
            var bytes = resource.getValue();

            try {
                switch (name) {
                    case String s when s.endsWith("MANIFEST.MF") -> new ManifestHandler().handle(zos, name, bytes);
                    default -> {
                        // Resource is not handled, write normally
                        ZipUtils.writeZipEntry(zos, resource.getKey(), resource.getValue());
                    }
                }
            } catch (IOException _) {}
        }
    }
}