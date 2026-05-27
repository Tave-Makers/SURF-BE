package com.tavemakers.surf.global.common.s3.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.tavemakers.surf.global.common.s3.dto.PreSignedUrlResDto;
import com.tavemakers.surf.global.common.s3.exception.FileNameIsEmptyException;
import com.tavemakers.surf.global.common.s3.exception.InvalidFileUrlException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    public static final String ORIGINAL_PATH = "original/";
    public static final Integer PRESIGNED_EXPIRATION = 2;

    private final AmazonS3 s3Client;
    @Value("${cloud.aws.bucket-name}")
    private String bucketName;

    /**
     * 다수 파일에 대한 PreSigned URL 목록 생성
     */
    public List<PreSignedUrlResDto> generatePreSignedUrlList(List<String> fileNames) {
        validateFileName(fileNames);
        return fileNames.stream()
                .map(this::generateSinglePutPreSignedUrl)
                .toList();
    }

    private void validateFileName(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new FileNameIsEmptyException();
        }
    }

    /**
     * 단일 파일 업로드용 PreSigned URL 생성
     */
    public PreSignedUrlResDto generateSinglePutPreSignedUrl(String filename) {
        String key = ORIGINAL_PATH + UUID.randomUUID() + "/" + filename;
        Date expiration = getExpiration();

        GeneratePresignedUrlRequest request =
                getPostGeneratePresignedUrlRequest(key, expiration);
        URL url = s3Client.generatePresignedUrl(request);

        return PreSignedUrlResDto.from(key, url.toString(), filename);
    }

    private GeneratePresignedUrlRequest getPostGeneratePresignedUrlRequest(String fileName, Date expiration) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest
                = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.PUT)
                .withKey(fileName)
                .withExpiration(expiration);
        return generatePresignedUrlRequest;
    }

    private Date getExpiration() {
        Instant expirationInstant = Instant.now().plus(PRESIGNED_EXPIRATION, ChronoUnit.MINUTES);
        return Date.from(expirationInstant);
    }

    /**
     * S3 파일 삭제 (파일 본체와 마커를 모두 정리)
     */
    public void deleteFile(String fileUrl) {
        String fileKey = extractKey(fileUrl); // original/{UUID}/{filename}

        // 실제 파일 삭제
        s3Client.deleteObject(bucketName, fileKey);

        // 부모 폴더 마커(original/{UUID}/) 정리
        int lastSlash = fileKey.lastIndexOf('/');
        if (lastSlash > 0) {
            String folderMarkerKey = fileKey.substring(0, lastSlash + 1);
            s3Client.deleteObject(bucketName, folderMarkerKey);
        }
    }

    /**
     * fileUrl에서 S3 객체 key를 추출한다.
     * full URL(https://bucket.s3.amazonaws.com/original/...) 과 key(original/...) 양쪽을 허용한다.
     * DB에는 full URL이 저장되는 것이 원칙이나, 프론트가 PreSignedUrlResDto.key를 직접 전달하는
     * 경우를 대비해 key 형식도 방어적으로 처리한다.
     */
    private String extractKey(String fileUrl) {
        try {
            String path = new URL(fileUrl).getPath();
            String key = path.startsWith("/") ? path.substring(1) : path;
            return validateKey(key);
        } catch (MalformedURLException e) {
            return validateKey(fileUrl);
        }
    }

    private String validateKey(String key) {
        if (key != null && key.startsWith(ORIGINAL_PATH)) {
            return key;
        }
        throw new InvalidFileUrlException();
    }

}
