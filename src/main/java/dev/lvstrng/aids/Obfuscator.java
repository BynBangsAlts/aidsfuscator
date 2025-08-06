package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Obfuscator {
    private final File input;
    public static int writerFlags;

    public Obfuscator(String input, int flags) {
        this.input = new File(input);
        writerFlags = flags;
    }

    public void readInput() {
        try (var zip = new ZipFile(input)) {
            for(var entry : zip.stream().toList()) {
                var in = zip.getInputStream(entry);

                if(entry.getName().contains(".class") || entry.getName().contains(".class/")) {
                    var node = readClass(in);
                    Jar.addClass(node);
                } else {
                    Jar.addResource(entry.getName(), in.readAllBytes());
                }
            }
        } catch (IOException _) {}
    }

    public void obfuscate(Transformer... transformers) {
        if(transformers == null)
            return;

        for(var transformer : transformers) {
            transformer.transform();
        }
    }

    public void saveOutput(String name) {
        var classes = new ArrayList<>(Jar.getClasses());
        classes.addAll(Jar.getArtificials().values());

        try (var zos = new ZipOutputStream(new FileOutputStream(name))) {
            for(var classNode : classes) {
                writeZipEntry(zos, classNode.name.replace('/', '.') + ".class", classToBytes(classNode));
            }

            for(var resource : Jar.getResources().entrySet()) {
                writeZipEntry(zos, resource.getKey(), resource.getValue());
            }

            zos.finish();
        } catch (IOException _) {}
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        if(data == null)
            return;

        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    public static ClassNode readClass(InputStream is) throws IOException {
        var node = new ClassNode();
        new ClassReader(is).accept(node, 0);
        return node;
    }

    public static byte[] classToBytes(ClassNode classNode) {
        var writer = new ClassWriter(writerFlags);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
