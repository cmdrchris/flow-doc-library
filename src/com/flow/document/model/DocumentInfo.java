package com.flow.document.model;

public class DocumentInfo {

    private final byte[] bytes;
    private final String bucket;
    private final String s3key;
    private final String srcKeyWithoutExtension;
    private final String documentId;

    public DocumentInfo(byte[] bytes, String bucket, String s3key) {
        this.bytes = bytes;
        this.bucket = bucket;
        this.s3key = s3key;
        srcKeyWithoutExtension = s3key.replace(".pdf", "");
        documentId = srcKeyWithoutExtension.replace("documents/", "");
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getSrcKeyWithoutExtension() {
        return srcKeyWithoutExtension;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getS3key() {
        return s3key;
    }
}
