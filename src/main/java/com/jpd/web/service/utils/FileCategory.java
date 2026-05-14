package com.jpd.web.service.utils;

public enum FileCategory {
    CERTIFICATE("certificate"),
    IMG("FileUpload/Img"),
    PDF("FileUpload/Pdf");
    
    private String path;
    
    FileCategory(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
}