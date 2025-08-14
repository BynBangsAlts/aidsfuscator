package dev.lvstrng.aids.jar.dependencies;

import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.zip.ZipFile;

public class Dependencies {
    public static void analyze(String path) {
        loadJDKClasses();

        var dependencyDir = new File(path);
        if(!dependencyDir.isDirectory())
            return;

        var files = dependencyDir.listFiles();
        if(files == null)
            return;

        for(var file : files) {
            if(!file.isFile() || !file.getName().endsWith(".jar"))
                continue;

            try (var zipFile = new ZipFile(file)) {
                for(var entry : Collections.list(zipFile.entries())) {
                    try (var in = zipFile.getInputStream(entry)) {
                        var name = entry.getName();

                        if(name.endsWith(".class")) {
                            var node = readAsDependency(in);
                            Jar.addLibrary(node);
                        }
                    }
                }
            } catch (IOException _) {}
        }
    }

    private static void loadJDKClasses() {
        try {
            FileSystem fs;
            try {
                fs = FileSystems.getFileSystem(URI.create("jrt:/"));
            } catch (FileSystemNotFoundException _) {
                fs = FileSystems.newFileSystem(URI.create("jrt:/"), Collections.emptyMap());
            }

            var modulesPath = fs.getPath("/modules");
            try (var modules = Files.walk(modulesPath)) {
                modules.filter(e -> e.toString().endsWith(".class"))
                        .forEach(e -> {
                            try (var in = Files.newInputStream(e)) {
                                var node = readAsDependency(in);
                                Jar.addLibrary(node);
                            } catch (IOException ignored) {}
                        });
            }
        } catch (IOException _) {}
    }

    private static ClassNode readAsDependency(InputStream in) throws IOException {
        var reader = new ClassReader(in);
        var node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

        return node;
    }
}
