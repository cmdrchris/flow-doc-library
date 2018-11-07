package com.flow.lambda;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.flow.document.callback.ApiLambdaCallback;
import com.flow.document.callback.CallbackHandler;
import com.flow.document.file.FileMover;
import com.flow.document.file.MoveToS3;
import com.flow.document.model.DocumentInfo;
import com.flow.document.render.PDFBoxRenderer;
import com.flow.document.render.PDFRenderer;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Optional;

public class RenderFunctionHandler implements RequestHandler<S3Event, Optional<Boolean>>  {
    Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    private final AmazonS3 s3Client = AmazonS3Client
            .builder()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();

    FileMover<InputStream, ObjectMetadata>  mover = new MoveToS3(s3Client);
    CallbackHandler<String> callbackHandler = new ApiLambdaCallback();
    PDFRenderer renderer = new PDFBoxRenderer(mover, callbackHandler);

    //entry point
    public Optional<Boolean> handleRequest(S3Event s3event, Context context) {
        return fetchDocument(s3event).map(documentInfo -> {
            try {
                renderer.processPDF(documentInfo);
            } catch (Exception e) {
                logger.error("failed processing " + s3event.toString(), e);
            }
            return Boolean.TRUE;
        });
    }

    //fetch file from s3 bucket
    private Optional<DocumentInfo> fetchDocument(S3Event s3event) {
        S3EventNotificationRecord record = s3event.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getKey().replace("+", " ");

        S3Object object = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
        try {
            srcKey = URLDecoder.decode(srcKey, "UTF-8");
            // Download S3 object on a server creating it each time this will be a bad Idea
            InputStream objectData = object.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectData);
            return Optional.of(new DocumentInfo(bytes, srcBucket, srcKey));
        } catch (IOException ioe) {
            logger.info("IOException raised fetching the document. " + ioe.getMessage());

        } finally {
            //Ignore the closing exception.
            try {
                object.getObjectContent().close();
            } catch (IOException e) {
            }
        }
        return Optional.empty();
    }


}


