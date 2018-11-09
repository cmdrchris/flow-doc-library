package com.flow.document.render;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.flow.document.callback.CallbackHandler;
import com.flow.document.file.FileMover;
import com.flow.document.model.DocumentInfo;
import com.foxit.gsdk.PDFException;
import com.foxit.gsdk.image.Bitmap;
import com.foxit.gsdk.pdf.*;
import com.foxit.gsdk.utils.*;
import com.sun.tools.javac.main.Option;
import common.Common;
import common.NativeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FoxitRenderer implements PDFRenderer {
    static Logger logger = LogManager.getLogger(FoxitRenderer.class);
    private final FileMover<InputStream, ObjectMetadata> mover;
    private final CallbackHandler<String> callbackHandler;

    String JPG_TYPE = "jpg";
    String JPG_MIME = "image/jpeg";
    String DST_BUCKET = System.getenv("BUCKET");
    String BUCKET_URL = System.getenv("BUCKET_URL");

    static {
        try {
            NativeUtils.loadLibraryFromJar("/libfsdk_java_linux64.so");
            logger.info("library loaded from the jar");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                String currentPath = new File(".").getCanonicalPath();
                System.load(currentPath + "/lib/libfsdk_java_linux64.so");
                System.out.println("local env");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }

    public FoxitRenderer(FileMover<InputStream, ObjectMetadata> mover, CallbackHandler<String> callbackHandler) {
        this.mover = mover;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void processPDF(DocumentInfo documentInfo) throws IOException {
        Common common = new Common();
        common.initlize();

        logger.debug("starting");
        long start = System.currentTimeMillis();
        logger.info("Starting foxit rendering of: " + documentInfo.getDocumentId());
        Optional<PDFDocument> maybeOpenDocu = Optional.empty();
        Optional<FileHandler> maybeOpenHandler = Optional.empty();
        try {
            logger.info("try");
            FileHandler handler = FileHandler.create(documentInfo.getBytes(), FileHandler.FILEMODE_READONLY);
            PDFDocument document = PDFDocument.open(handler, null);
            int pageCount = document.countPages();
            logger.info("this pdf has " + pageCount + " pages.");
            List<String> pageParams = new ArrayList<>();

            for (int i = 0; i < pageCount; i++) {
                PDFPage page = document.getPage(i);
                Bitmap bmp = renderPageToBitmap(page);

                String imagePath =  documentInfo.getSrcKeyWithoutExtension() + "_page_" + i + "." + JPG_TYPE;
                BufferedImage finalImage = bmp.convertToBufferedImage();

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(finalImage, "gif", os);
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                // Set Content-Length and Content-Type
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(os.size());
                meta.setContentType(JPG_MIME);

                mover.moveToDestination(DST_BUCKET, imagePath, is, meta);
                logger.debug("  Writing to: " + imagePath);
                float progress = (i+1) / pageCount;
                String bucketPlusImagePath = BUCKET_URL + imagePath;

                String pageParamStr = "source_url=" + documentInfo.getS3key()
                        + "&pages[][total_pages]=" + pageCount
                        + "&pages[][progress]="+ progress
                        + "&pages[][index]=" + i
                        + "&pages[][source]=s3&pages[][source_url]="
                        + bucketPlusImagePath + "&pages[][width]="
                        + finalImage.getWidth() + "&pages[][height]=" + finalImage.getHeight();
                pageParams.add(pageParamStr);
            }
            logger.info("params " + pageParams.size());
            long elapsed = System.currentTimeMillis() - start;
            logger.info("" + pageCount + " pages processed in " + elapsed + "ms.");
            maybeOpenDocu = Optional.of(document);
            maybeOpenHandler = Optional.of(handler);

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

            logger.debug("I am sending "+ chunks.size() + "to flow API");
            chunks.stream().forEach(chunk -> callbackHandler.doCallback(String.join("&", chunk)));
            logger.debug("Document has " + pageCount + " pages");
            logger.info("done");
        } catch (PDFException e) {
            logger.error("FUUUUUUUU something went wrong", e);
        } finally {
            try {
                if(maybeOpenDocu.isPresent()) {
                    maybeOpenDocu.get().close();
                }
                if(maybeOpenHandler.isPresent()){
                    maybeOpenHandler.get().release();
                }
            } catch (PDFException pe) {
                //do nothing
            }
        }

    }


    public Bitmap renderPageToBitmap(PDFPage pPage) {
        Bitmap bmp = null;
        try {
            Progress progress = pPage.startParse(PDFPage.RENDERFLAG_NORMAL);
            if (progress != null) {
                int ret = Progress.TOBECONTINUED;
                while (ret == Progress.TOBECONTINUED) {
                    ret = progress.continueProgress(30);
                }
            }
            progress.release();
            SizeF pageSize = pPage.getSize();
            Matrix matrix = new Matrix();
            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();
            matrix = pPage.getDisplayMatrix(0, 0, width, height, 0);
            bmp = Bitmap.create(new Size(width, height), Bitmap.FORMAT_24BPP_BGR, null, 0);
            Rect rect = new Rect(0, 0, width, height);
            bmp.fillRect(0xffffffffL, rect);
            Renderer render = Renderer.create(bmp);
            RenderContext renderContext = RenderContext.create();
            renderContext.setMatrix(matrix);
            renderContext.setFlags(RenderContext.FLAG_ANNOT);
            Progress renderProgress = pPage.startRender(renderContext, render, 0);
            if (renderContext != null) {
                int ret = Progress.TOBECONTINUED;
                while (ret == Progress.TOBECONTINUED) {
                    ret = renderProgress.continueProgress(30);
                }
            }
            renderProgress.release();
        } catch (PDFException e) {
            e.printStackTrace();
            Common.outputErrMsg(e.getLastError(), "Failed to render page to bitmap.");
        }
        return bmp;
    }
}
