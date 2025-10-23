package com.app.zip.service;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class ZipService {

    private static final int BUFFER_SIZE = 16384;

    @Autowired
    private GoogleDriveService driveService;

    public void compressFilesToTempAndDownload(List<MultipartFile> files, HttpServletResponse response)
            throws IOException {

        // 1️⃣ Create temporary ZIP file
        File zipTemp = File.createTempFile("upload_", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipTemp);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

            byte[] buffer = new byte[BUFFER_SIZE];

            for (MultipartFile file : files) {
                zos.putNextEntry(new ZipEntry(file.getOriginalFilename()));

                try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
                    int length;
                    while ((length = bis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                zos.closeEntry();
            }
        }

        // 2️⃣ Upload to Google Drive
        try {
            String driveLink = driveService.uploadFile(zipTemp);
            System.out.println("✅ Uploaded to Drive: " + driveLink);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3️⃣ Stream ZIP back to client
        String zipFileName = (files.size() == 1)
                ? files.get(0).getOriginalFilename().replaceAll("\\.[^.]+$", "") + ".zip"
                : "compressed.zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        try (InputStream is = new FileInputStream(zipTemp);
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } finally {
            zipTemp.delete(); // cleanup temp file
        }
    }
}
