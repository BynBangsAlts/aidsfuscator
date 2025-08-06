package dev.lvstrng.aids.jar;

public class DependencyMissingException extends RuntimeException {
    public DependencyMissingException(String className) {
        super("Class '" + className + "' not found. Please install the correct libraries");
    }
}
