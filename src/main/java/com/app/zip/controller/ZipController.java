package com.app.zip.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.app.zip.service.ZipService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/zip")
public class ZipController {

	@Autowired
	private ZipService zipService;

	@PostMapping(value = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void compressFiles(@RequestParam("files") MultipartFile[] files, HttpServletResponse response)
			throws IOException {

		List<MultipartFile> fileList = Arrays.asList(files);
		zipService.compressFilesToTempAndDownload(fileList, response);
	}
}
