package com.couple.ordering.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务。
 */
public interface FileService {

    /**
     * 上传图片，返回可访问的相对 URL。
     */
    String uploadImage(MultipartFile file);
}
