package com.agms.backend.model;

public class FileUploadResponse {
    private String filename;
    private String message;
    private String downloadUrl;

    public FileUploadResponse(String filename, String message, String downloadUrl) {
        this.filename = filename;
        this.message = message;
        this.downloadUrl = downloadUrl;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
} 