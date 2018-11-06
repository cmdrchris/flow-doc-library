package com.flow.document.file;

public interface FileMover<T,U> {
    void moveToDestination(String path, String objectName, T objext, U metadata);
}
