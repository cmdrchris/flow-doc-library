package com.flow.document.callback;

public interface CallbackHandler<T> {
    void doCallback(T data);
}
