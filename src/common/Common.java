package common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.foxit.gsdk.PDFException;
import com.foxit.gsdk.PDFLibrary;
import com.foxit.gsdk.pdf.PDFDocument;
import com.foxit.gsdk.pdf.PDFPage;
import com.foxit.gsdk.pdf.Progress;
import com.foxit.gsdk.utils.FileHandler;

public class Common {
    /**
     * license file
     */
    private static String license_id = "WBPWiUbO2wbIzg67C9ERBH+bNcape1rVUFxOqmTTYHiRwdXvzICl/w==";
    private static String unlockCode = "8f3o1kFsvWkNAgdStNFabUId0OUDXcELMVDQ43CsI9S4ZX13eBFLNhB5t8Ehr5JB7BfX92h12mrO6jGYSt6Y8DU2hGRXWJlZ3dtsKfzFcPJVjsrcAw0O+oNmFRdv2Aq/C/2xzae9yDrJwNd0/jkGTPSZhUzqkPTuigdzzkXIYJIpeNKXqjspSVDSutVRkvRKA/B8HjyHrxUYZagIwiqls0y3ANTHVqg+xjiHROXv/MjjIOXjF4bjQdnzvYTP/UedXRqrAjsves+gmOoWbncSdf/mkAp8IQkR/4GcJbanUkY3LXv1bPJQWeXa3hHostUWIQUHSZaTiICiZ5KQ418A3wII5ItFkIngj9yN0gFNnI2sZY0KUZLlKXHOiHFZBxGCiXn6Vm/MnsPkBJxg7njjZtcHOjy73CHuE1+axpXPbyRro97PrNz1dWD1h6+7NLW2+grrlhfwjnDYiUxy49d5jkl00jBReAt9OxVucKxJvKH++Qc7V4AY4gngewG0g5btpot3o6bi7fhCkkIuZp1EYqUtEQlT/pm99hhsR26XSMa74tNxELRf7qnfw3ybVA58fu20stH7rSarctPWUODbOrk1y5CJIRS1wZQ2hMeyLTCfAi0//8l1xrd8otNq5zwiZho/3e0VG0IGl6Dz+LGkPgUY5fZj+SNJPo+kl4coK477/DNjFWTaRxtDEK+/Qvn/gCMp/iaJChsVn5cf51+y/5GlJQQ60dd/kQLUOqiChygTcAga1ExGYMIfHfZLFZ6CojEJPDyaigblx3vDMG10dBqthdyeI6v73AFYQQxsPsdmHcPe2Ss9laJlJ/ZhcAriQ8WeBzlUab0yE4ca7CYTVKqVy+M341WgsC7VBDgFiXJl9j2JKwOER/U/bcKzasKeV4ANBrWmBdNrO2j0r6DMpD3NFmOR4Yqs7F0HM1x9h613sSrfnnYwtt3AJKztX5W4hsuhbaOMfAJOBShraWMEdOZUIDwtQx5WFYnfnS94f+AXADP7xOk0gDiEQaGcUbxlc796wzBNSzGaeKVKqsqQu7qvNUBB/xAkQo/OrOyvINBAjdyrYOpMc60RzKMlyK8BL9pRx9fdnOEJOaVxzzKH/U6X7GW2opgq4x2Hi4tBaWdGlZaMe/JBDI+e3RwS1yqZqDQYZbFVJ1KMHq4rWYuet9K1uAI2ywP1nK4hWPVesjogc9YHC6Wb5zsS6puXFIbGlAHVYb2vmcLpw2jAZ8ZoQwTxSHqrzMkN6Z6Z5i172v1Aro/u5bXVQ2IcQPM4uHV2dfnNljMQR4u2SFwhPB4+m7KEd4VYKH7q2W/KL122obVccZX5kd6dSeRKDJCbYGWcCtHzA7EKr/cFWXH9XSGmwnlzI/4=";
    private int memorySize = 8 * 1024 * 1024;
    private boolean scaleable = true;

    private static FileHandler handler = null;
    private static FileOutputStream stream = null;

    public static void openLog(String path) {
        File file = new File(path);

        try {
            if (!file.exists() || file.isDirectory()) {
                file.createNewFile();
            }

            stream = new FileOutputStream(file, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void outputLog(String log) {
        if (log == null || log.length() < 1)
            System.out.println("Please input log content.");

        System.out.println(log);

        if (stream == null)
            return;
        synchronized (stream) {

            try {
                stream.write(log.getBytes());
                stream.write("\r\n".getBytes());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void closeLog() {
        if (stream == null)
            return;
        try {
            stream.flush();
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        stream = null;
    }

    //this function will output the specified message according to parameter, mainly used in PDFException catching.
    public static void outputErrMsg(int err, String msg) {
        if (err == PDFException.ERRCODE_INVALIDLICENSE)
            outputLog("Invalid license!!! Please check whether the license has related module!!!");
        else
            outputLog(msg + " Error code:" + err);
    }

    public void initlize() {
        PDFLibrary pdfLibrary = PDFLibrary.getInstance();
        try {
            pdfLibrary.initialize(memorySize, scaleable);
            pdfLibrary.unlock(license_id, unlockCode);
            outputLog("Success: Initialize and unlock the library.");
            int type = pdfLibrary.getLicenseType();
            if (type == PDFLibrary.LICENSETYPE_EXPIRED || type == PDFLibrary.LICENSETYPE_INVALID) {
                outputLog("License is invalid or expired!!!");
                System.exit(1);
            }
        } catch (PDFException e) {
            e.printStackTrace();
            outputErrMsg(e.getLastError(), "Failed to initlize and unlock the library");
            System.exit(1);// exit
        }
    }

    public void release() {
        if (handler != null) {
            try {
                handler.release();
            } catch (PDFException e) {
                e.printStackTrace();
                outputErrMsg(e.getLastError(), "Failed to rlease file handle.");
            } finally {
                PDFLibrary pdfLibrary = PDFLibrary.getInstance();
                pdfLibrary.destroy();
            }
        } else {
            PDFLibrary pdfLibrary = PDFLibrary.getInstance();
            pdfLibrary.destroy();
        }
    }

    public static PDFDocument createDocument() {
        PDFDocument pdfDocument = null;
        try {
            pdfDocument = PDFDocument.create();
            outputLog("Success: Create a PDF document.");
        } catch (PDFException e) {
            e.printStackTrace();
            outputErrMsg(e.getLastError(), "Failed to create PDF Doument.");
            System.exit(1);// exit
        }

        return pdfDocument;
    }

    public static PDFDocument openDocument(String filePath, String password) {
        PDFDocument pdfDocument = null;
        try {
            handler = FileHandler.create(filePath, FileHandler.FILEMODE_READONLY);
            if (password == null) {
                pdfDocument = PDFDocument.open(handler, null);
            } else {
                pdfDocument = PDFDocument.open(handler, password.getBytes());
            }
            outputLog("Success: Open an existing PDF document file.");
        } catch (PDFException e) {
            e.printStackTrace();
            outputErrMsg(e.getLastError(), "Failed to open PDF Doument.");
            System.exit(1);// exit
        }

        return pdfDocument;
    }

    public static PDFPage getPage(PDFDocument pdfDocument, int index) {
        PDFPage page = null;
        try {
            page = pdfDocument.getPage(index);
            outputLog("Success: Get a PDF page.");
        } catch (PDFException e) {
            e.printStackTrace();
            outputErrMsg(e.getLastError(), "Failed to get PDF Page.");
            System.exit(1);// exit
        }

        return page;
    }

    public static PDFPage createPage(PDFDocument pdfDocument, int index) {
        PDFPage page = null;
        try {
            page = pdfDocument.createPage(index);
            outputLog("Success: Create a PDF page.");
        } catch (PDFException e) {
            e.printStackTrace();
            outputErrMsg(e.getLastError(), "Failed to create PDF Page.");
            System.exit(1);// exit
        }
        return page;
    }

    public static boolean fileExist(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile())
            return true;
        else {
            return false;
        }
    }

    public static boolean folderExist(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory())
            return true;
        else {
            return false;
        }
    }

    public static String getFileName(String path) {
        int index = path.lastIndexOf("/");

        String fileName = path.substring(index + 1, path.length());
        return fileName;
    }

    public static String getFileNameNoEx(String filename) {
        String pFileName = null;
        if ((filename != null) && (filename.length() > 0)) {
            File tempFile = new File(filename.trim());
            pFileName = tempFile.getName();
            int dot = pFileName.lastIndexOf('.');
            if ((dot > -1) && (dot < (pFileName.length()))) {
                return pFileName.substring(0, dot);
            }
        }
        return pFileName;
    }

    public static boolean createFloder(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) return true;
        boolean bCreate = false;
        bCreate = file.mkdir();

        return bCreate;
    }

    public static void saveDocument(PDFDocument pdfDocument, String path) {
        FileHandler handler = null;
        try {
            handler = FileHandler.create(path, FileHandler.FILEMODE_TRUNCATE);
            Progress progress = pdfDocument.startSaveToFile(handler, PDFDocument.SAVEFLAG_OBJECTSTREAM);
            if (progress != null) {
                progress.continueProgress(0);
            }

            progress.release();
            handler.release();
            outputLog("Success: Save PDF document.");
        } catch (PDFException e) {
            e.printStackTrace();
            if (handler != null) {
                try {
                    handler.release();
                } catch (PDFException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            outputErrMsg(e.getLastError(), "Failed to save PDF Document.");
            System.exit(1);// exit
        }
    }
}
