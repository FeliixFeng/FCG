package com.ghf.fcg.common.controller;

import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.common.utils.OssUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
@Tag(name = "文件存储", description = "OSS 上传接口")
public class OssController {

    private final OssUtils ossUtils;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<String> upload(@RequestPart("file") MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "common") String dir) {
        String url = ossUtils.upload(file, dir);
        return Result.success(url);
    }
}
