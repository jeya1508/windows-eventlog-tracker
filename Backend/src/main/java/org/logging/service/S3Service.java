package org.logging.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class S3Service {
    private final AmazonS3 s3Client;
    private final String bucketName;
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    public S3Service(String bucketName) {
        AWSCredentials credentials = new BasicAWSCredentials(System.getenv("AWS_ACCESS_KEY"),System.getenv("AWS_SECRET_ACCESS_KEY"));

        this.s3Client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.AP_SOUTH_1)
                .build();

        this.bucketName = bucketName;

    }
    public void uploadCSVToS3(String filePath, String s3Key) {
        File file = new File(filePath);
        if (file.exists()) {
            s3Client.putObject(new PutObjectRequest(bucketName, s3Key, file));
            logger.info("File uploaded successfully to S3 with key:{}" , s3Key);
        } else {
            logger.error("File not found:{} " , filePath);
        }
    }

    public void deleteAllCSVFilesFromS3() {
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result = s3Client.listObjectsV2(req);

        List<S3ObjectSummary> objects = result.getObjectSummaries();
        for (S3ObjectSummary os : objects) {
            if (os.getKey().endsWith(".csv")) {
                s3Client.deleteObject(new DeleteObjectRequest(bucketName, os.getKey()));
                logger.info("Deleted file: {} " , os.getKey());
            }
        }
    }
    public void deleteCSVFileFromS3(String fileName) {
        try {
            String s3Key = "exports/" + fileName;

            if (s3Client.doesObjectExist(bucketName, s3Key)) {
                s3Client.deleteObject(new DeleteObjectRequest(bucketName, s3Key));
                logger.info("Removed {}" ,s3Key +" successfully");
            } else {
                logger.error("File not found in S3:{} " , s3Key);
            }
        } catch (Exception e) {
            logger.error("Error deleting file from S3:{} " , e.getMessage());
        }
    }
    public List<String> getAllCSVFilesFromS3() {
        List<String> csvFileNames = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName);
        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                if (objectSummary.getKey().endsWith(".csv")) {
                    csvFileNames.add(objectSummary.getKey());
                }
            }

            request.setContinuationToken(result.getNextContinuationToken());

        } while (result.isTruncated());
        Collections.reverse(csvFileNames);
        return csvFileNames;
    }
    public String generatePreSignedURL(String fileName) {
        try {
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60;
            expiration.setTime(expTimeMillis);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

            return url.toString();
        } catch (Exception e) {
           logger.error("Error generating pre-signed URL:{}" ,e.getMessage());
            return null;
        }
    }
}
