package com.flow.document.file;

import java.io.InputStream;

public interface FileMover<T,U> {
    void moveToDestination(String path, String objectName, T objext, U metadata);
}
