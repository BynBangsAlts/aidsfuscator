package dev.lvstrng.aids.jar.resources;

import java.util.zip.ZipOutputStream;

/**
 * Handler class for resources that are affected by name obfuscation
 */
public interface HandledResource {
    void handle(ZipOutputStream zos, String name, byte[] bytes);
}
