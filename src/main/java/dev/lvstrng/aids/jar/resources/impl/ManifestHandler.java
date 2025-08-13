package dev.lvstrng.aids.jar.resources.impl;

import dev.lvstrng.aids.jar.resources.HandledResource;
import dev.lvstrng.aids.transform.impl.rename.Mappings;
import dev.lvstrng.aids.utils.ZipUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipOutputStream;

/**
 * Replace main class attribute after renaming
 */
public class ManifestHandler implements HandledResource {
    @Override
    public void handle(ZipOutputStream zos, String name, byte[] bytes) {
        try {
            var is = new ByteArrayInputStream(bytes);
            var manifest = new Manifest(is);

            var attributes = manifest.getMainAttributes();
            var main = attributes.getValue("Main-Class");

            if(main == null)
                return;

            var newName = Mappings.CLASS.newOrCurrent(main.replace('.', '/'));
            attributes.put(new Attributes.Name("Main-Class"), newName.replace('/', '.'));

            ZipUtils.writeManifest(zos, name, manifest);
        } catch (IOException e) {
            System.err.println("Failed to handle manifest: " + e.getMessage());
        }
    }
}
