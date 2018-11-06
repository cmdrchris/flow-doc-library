package com.flow.document.render;

import com.flow.document.model.DocumentInfo;

import java.io.IOException;

public interface PDFRenderer {
    void processPDF(DocumentInfo documentInfo) throws IOException;
}
