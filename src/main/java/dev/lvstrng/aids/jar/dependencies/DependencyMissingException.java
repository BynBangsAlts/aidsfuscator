package dev.lvstrng.aids.jar.dependencies;

public class DependencyMissingException extends RuntimeException {
    public DependencyMissingException(String className) {
        super("Class '" + className + "' not found. Please install the correct libraries");
    }
}
