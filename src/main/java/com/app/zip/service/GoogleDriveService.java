package com.app.zip.service;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "ZipUploaderClient";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private Drive driveService;

    public GoogleDriveService() {
        // Do NOT initialize Drive in constructor to avoid OAuth prompt at app start
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        if (driveService != null) return driveService;

//        InputStream in = getClass().getResourceAsStream("/credentials.json");
        InputStream in = new ByteArrayInputStream(System.getenv("SERVICE_ACCOUNT_JSON").getBytes());

        if (in == null) throw new FileNotFoundException("Resource not found: credentials.json");

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        // Local server receiver for OAuth callback
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
//                .setPort(8888) // or any free port
//                .setCallbackPath("/oauth2callback")
//                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setHost("zipuploader.vercel.app")
                .setPort(-1)
                .setCallbackPath("/oauth2callback")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return driveService;
    }

    public String uploadFile(File localFile) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();

        // Google Drive file metadata
        com.google.api.services.drive.model.File fileMetadata =
                new com.google.api.services.drive.model.File();
        fileMetadata.setName(localFile.getName());

        // File content
        FileContent mediaContent = new FileContent("application/zip", localFile);

        // Upload
        com.google.api.services.drive.model.File uploadedFile = drive.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute();

        return uploadedFile.getWebViewLink();
    }
}
