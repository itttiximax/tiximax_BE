package com.tiximax.txm.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final RestTemplate restTemplate;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;

    public ImageStorageService(RestTemplate restTemplate,
                               @Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.key}") String supabaseKey,
                               @Value("${supabase.bucket}") String bucketName) {
        this.restTemplate = restTemplate;
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.bucketName = bucketName;
    }

    public String uploadImage(MultipartFile file) throws IOException {

        String customFileName = "user-avatar-" + System.currentTimeMillis() +
                (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                        : ".png");
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + customFileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType() != null ? file.getContentType() : "image/png"));
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + customFileName;
        } else {
            throw new IOException("Upload thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public String uploadImageSupabase(MultipartFile file, String code) throws IOException {

        String customFileName = code + System.currentTimeMillis() +
                (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                        : ".png");
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + customFileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType() != null ? file.getContentType() : "image/png"));
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + customFileName;
        } else {
            throw new IOException("Upload ảnh thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
}