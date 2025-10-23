package com.app.zip.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "ZipUploaderClient";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private Drive driveService;

    public GoogleDriveService() {
        // Drive will be initialized lazily
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        if (driveService != null) return driveService;

        // Load service account JSON from environment variable
        String saJson = System.getenv("SERVICE_ACCOUNT_JSON");
        if (saJson == null || saJson.isEmpty()) {
            throw new IllegalStateException("Environment variable SERVICE_ACCOUNT_JSON is not set");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(saJson.getBytes()))
                .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();

        return driveService;
    }

    public String uploadFile(java.io.File localFile) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();

        // Use fully qualified name to avoid conflict
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(localFile.getName());

        FileContent mediaContent = new FileContent("application/zip", localFile);

        com.google.api.services.drive.model.File uploadedFile = drive.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute();

        return uploadedFile.getWebViewLink();
    }
}
