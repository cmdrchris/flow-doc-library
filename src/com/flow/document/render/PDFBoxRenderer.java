package com.flow.document.render;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.flow.document.callback.ApiLambdaCallback;
import com.flow.document.callback.CallbackHandler;
import com.flow.document.file.FileMover;
import com.flow.document.model.DocumentInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PDFBoxRenderer implements PDFRenderer {
    Logger logger = LogManager.getLogger(PDFBoxRenderer.class);

    private final FileMover<InputStream, ObjectMetadata> mover;
    private final CallbackHandler<String> callbackHandler;

    String JPG_TYPE = "jpg";
    String JPG_MIME = "image/jpeg";
    String DST_BUCKET = System.getenv("BUCKET");
    String BUCKET_URL = System.getenv("BUCKET_URL");

    public PDFBoxRenderer(FileMover<InputStream, ObjectMetadata> mover, CallbackHandler<String> callbackHandler){
        this.mover = mover;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void processPDF(DocumentInfo documentInfo) throws IOException {
        // Parse PDF document
        logger.info("got document : " + documentInfo.getS3key());
        logger.info("got document : " + documentInfo.getBytes());

        PDDocument doc = PDDocument.load(documentInfo.getBytes());
        int numberOfPages = doc.getNumberOfPages();

        // Render PDF pages
        org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.getText(doc);

        // Generate page previews before submitting to API
        logger.debug("Generating " + numberOfPages + " page previews");
        List<String> pageParams = new ArrayList<>();
        for (int i = 0; i < numberOfPages; i++) {
            BufferedImage renderedPage = renderer.renderImage(i, 2);
            String imagePath = documentInfo.getSrcKeyWithoutExtension() + "-page-" + i + ".jpg";

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(renderedPage, JPG_TYPE, os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            // Set Content-Length and Content-Type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(os.size());
            meta.setContentType(JPG_MIME);

            String bucketPlusImagePath = BUCKET_URL + imagePath;
            logger.debug("  Writing to: " + imagePath);

            mover.moveToDestination(DST_BUCKET, imagePath, is, meta);

            float progress = (i+1) / numberOfPages;

            String pageParamStr = "source_url=" + documentInfo.getS3key()
                    + "&pages[][total_pages]=" + numberOfPages
                    + "&pages[][progress]="+ progress
                    + "&pages[][index]=" + i
                    + "&pages[][source]=s3&pages[][source_url]="
                    + bucketPlusImagePath + "&pages[][width]="
                    + renderedPage.getWidth() + "&pages[][height]=" + renderedPage.getHeight();
            pageParams.add(pageParamStr);
        }
        doc.close();

        //TODO maybe split this out, this is not render specific we need this other renderers.
        // Chunk page params create urls parameters for pages grouped in 4
        logger.debug("Page previews generated: " + pageParams.size());
        int chunkSize = (int)Math.ceil((float)pageParams.size() / 4.0);
        logger.debug("Chunking page params. " + chunkSize + " requests per chunk");

        ArrayList<ArrayList> chunks = new ArrayList<>();
        chunks.add(new ArrayList<String>());

        int chunkIndex = 0;
        for (int i = 0; i < pageParams.size(); i++) {
            if ((i % chunkSize == 0) && (i != 0)) {
                ArrayList<String> newArray = new ArrayList<>();
                newArray.add(pageParams.get(i));
                chunks.add(newArray);
                chunkIndex++;
            } else {
                chunks.get(chunkIndex).add(pageParams.get(i));
            }
        }

        chunks.stream().forEach(chunk -> callbackHandler.doCallback(String.join("&", chunk)));
        logger.debug("Document has " + numberOfPages + " pages");
        logger.info("done");
    }
}
