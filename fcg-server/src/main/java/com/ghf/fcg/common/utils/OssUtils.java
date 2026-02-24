package com.ghf.fcg.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.config.OssProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OssUtils {

    private final OssProperties properties;
    private volatile OSS client;

    public String upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getBucket())) {
            throw new BusinessException(MessageConstant.OSS_CONFIG_MISSING);
        }

        String objectKey = buildObjectKey(file.getOriginalFilename(), dir);
        String endpoint = normalizeEndpoint(properties.getEndpoint());
        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setConnectionTimeout(5000);
        config.setSocketTimeout(10000);
        config.setConnectionRequestTimeout(5000);
        config.setMaxErrorRetry(1);
        try {
            getClient(endpoint, config).putObject(properties.getBucket(), objectKey, file.getInputStream());
            return buildUrl(objectKey);
        } catch (OSSException e) {
            throw new BusinessException(MessageConstant.OSS_UPLOAD_FAILED + ": " + e.getErrorMessage());
        } catch (ClientException e) {
            throw new BusinessException(MessageConstant.OSS_UPLOAD_FAILED + ": " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(MessageConstant.OSS_UPLOAD_FAILED);
        }
    }

    private OSS getClient(String endpoint, ClientBuilderConfiguration config) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new OSSClientBuilder().build(
                            endpoint,
                            properties.getAccessKeyId(),
                            properties.getAccessKeySecret(),
                            config
                    );
                }
            }
        }
        return client;
    }

    private String buildObjectKey(String originalFilename, String dir) {
        String fileName = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            fileName += ext;
        }

        String prefix = StringUtils.hasText(properties.getPrefix()) ? properties.getPrefix().trim() : "";
        String folder = StringUtils.hasText(dir) ? dir.trim() : "";
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(prefix)) {
            key.append(trimSlashes(prefix)).append('/');
        }
        if (StringUtils.hasText(folder)) {
            key.append(trimSlashes(folder)).append('/');
        }
        key.append(fileName);
        return key.toString();
    }

    private String buildUrl(String objectKey) {
        String domain = properties.getDomain();
        if (StringUtils.hasText(domain)) {
            return trimTrailingSlash(domain) + "/" + objectKey;
        }
        String endpoint = normalizeEndpoint(properties.getEndpoint());
        String endpointHost = endpoint.replaceFirst("^https?://", "");
        return "https://" + properties.getBucket() + "." + endpointHost + "/" + objectKey;
    }

    private String normalizeEndpoint(String endpoint) {
        String value = endpoint.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    private String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }
}
