package com.douding.file.controller.admin;

import com.douding.server.dto.FileDto;
import com.douding.server.dto.ResponseDto;
import com.douding.server.service.FileService;
import com.douding.server.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Objects;

/*
    返回json 应用@RestController
    返回页面  用用@Controller
 */
@RequestMapping("/admin/file")
@RestController
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
    public static final String BUSINESS_NAME = "文件上传";
    @Resource
    private TestService testService;

    @Value("${file.path}")
    private String FILE_PATH;

    @Value("${file.domain}")
    private String FILE_DOMAIN;

    @Resource
    private FileService fileService;

    @RequestMapping("/upload")
    public ResponseDto<FileDto> upload(@RequestBody FileDto fileDto) throws Exception {
        ResponseDto<FileDto> responseDto = new ResponseDto<>();

        fileService.upload(fileDto, FILE_PATH);

        if (Objects.equals(fileDto.getShardIndex(), fileDto.getShardTotal())) {
            FileDto merge = merge(fileDto);
            responseDto.setContent(merge);
        }
        return responseDto;
    }

    //合并分片
    public FileDto merge(FileDto fileDto) throws Exception {
        LOG.info("合并分片开始");
        return fileService.merge(fileDto, FILE_PATH, FILE_DOMAIN);
    }

    @GetMapping("/check/{key}")
    public ResponseDto<FileDto> check(@PathVariable String key) throws Exception {
        LOG.info("检查上传分片开始：{}", key);
        ResponseDto<FileDto> responseDto = new ResponseDto<>();
        FileDto fileDto = fileService.check(key, FILE_PATH, FILE_DOMAIN);
        if (fileDto == null) {
            return responseDto;
        }
        responseDto.setContent(fileDto);
        return responseDto;
    }

}//end class
