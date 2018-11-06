package com.flow.document.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.InputStream;

public class MoveToS3 implements FileMover<InputStream, ObjectMetadata> {
    private final AmazonS3 s3Client;

    public MoveToS3(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void moveToDestination(String destination, String imagePath, InputStream is, ObjectMetadata meta) {
        s3Client.putObject(destination, imagePath, is, meta);
    }
}
