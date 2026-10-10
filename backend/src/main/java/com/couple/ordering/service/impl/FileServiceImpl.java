package com.couple.ordering.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * 文件上传服务实现。
 *
 * <p>校验格式（jpg/jpeg/png/webp）与 Content-Type，UUID 重命名，按年月分目录存储；
 * 返回以 /uploads 开头的相对 URL，由 Nginx（prod）或后端资源映射（dev）托管。</p>
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    @Value("${couple.upload.dir}")
    private String uploadDir;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.UPLOAD_ERROR, "请选择要上传的图片");
        }
        String originalFilename = StrUtil.nullToEmpty(file.getOriginalFilename());
        String extension = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.UPLOAD_ERROR,
                    "仅支持 jpg、jpeg、png、webp 格式的图片");
        }
        // 双保险：同时校验 Content-Type，防止改扩展名绕过
        if (file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(ResultCode.UPLOAD_ERROR, "文件类型不正确");
        }
        String yearMonth = DateUtil.format(new java.util.Date(), "yyyyMM");
        String filename = IdUtil.fastSimpleUUID() + "." + extension;
        File target = new File(uploadDir, yearMonth + File.separator + filename);
        try {
            FileUtil.mkdir(target.getParentFile());
            file.transferTo(target.getAbsoluteFile());
        } catch (IOException e) {
            log.error("图片上传失败：{}", originalFilename, e);
            throw new BusinessException(ResultCode.UPLOAD_ERROR);
        }
        return "/uploads/" + yearMonth + "/" + filename;
    }
}
