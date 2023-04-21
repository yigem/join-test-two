package com.douding.server.service;

import com.douding.server.domain.File;
import com.douding.server.domain.FileExample;
import com.douding.server.dto.FileDto;
import com.douding.server.dto.PageDto;
import com.douding.server.mapper.FileMapper;
import com.douding.server.util.CopyUtil;
import com.douding.server.util.UuidUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Resource
    private FileMapper fileMapper;

    public void upload(MultipartFile file, String filePath) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd/");
        String s = format.format(date);

        String fileFolderPath = filePath + s;
        java.io.File dir = new java.io.File(fileFolderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        java.io.File f = new java.io.File(fileFolderPath, MD5Encoder.encode(file.getBytes()) + ".png");
        boolean newFile = f.createNewFile();
        if (newFile) {
            file.transferTo(f);
        }
    }

    // 文件上传
    public void upload(FileDto fileDto, String filePath) throws IOException {
        String key = fileDto.getKey();
        Integer shardIndex = fileDto.getShardIndex();
        byte[] bytes = Base64.decodeBase64(fileDto.getShard().replace("data:application/octet-stream;base64,", ""));

        String chunkFolderPath = getChunkFolderPath(filePath, key);
        java.io.File dir = new java.io.File(chunkFolderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        java.io.File file = new java.io.File(chunkFolderPath, shardIndex.toString());
        boolean newFile = file.createNewFile();
        if (newFile) {
            try (
                    RandomAccessFile write = new RandomAccessFile(file, "rw");
            ) {
                // 保存分片到磁盘
                write.write(bytes);
            }
        }
    }

    // 文件合并
    public FileDto merge(FileDto fileDto, String filePath, String fileDomain) throws IOException {
        String key = fileDto.getKey();
        String fileFolderPath = getFilePath(filePath, key);
        String chunkFolderPath = getChunkFolderPath(filePath, key);

        java.io.File chunkFolder = new java.io.File(chunkFolderPath);

        java.io.File mergeFile = new java.io.File(fileFolderPath, key + "." + fileDto.getSuffix());
        mergeFile.createNewFile();

        List<java.io.File> files = Arrays.stream(chunkFolder.listFiles())
                .sorted(Comparator.comparing(java.io.File::getName))
                .collect(Collectors.toList());

        try (
                RandomAccessFile write = new RandomAccessFile(mergeFile, "rw")
        ) {
            byte[] bytes = new byte[1024];
            for (java.io.File file : files) {
                try (
                        RandomAccessFile read = new RandomAccessFile(file, "r");
                ) {
                    int len;
                    while ((len = read.read(bytes)) != -1) {
                        write.write(bytes, 0, len);
                    }
                }
            }
        }

        String fileRelativePath = getFileRelativePath(key, fileDto.getSuffix());
        fileDto.setPath(fileRelativePath);
        this.save(fileDto);

        FileDto dto = new FileDto();
        dto.setPath(fileDomain + fileRelativePath);
        return dto;
    }

    /**
     * 判断分片是否存在
     *
     * @param key 图片信息 md5 摘要值
     * @return 返回下一块分片号
     * @throws FileNotFoundException
     */
    public FileDto check(String key, String filePath, String fileDomain) throws FileNotFoundException {
        FileDto fileDto = new FileDto();
        // 判断数据库中是否存在
        fileDto = this.findByKey(key);
        if (fileDto != null && !StringUtils.isEmpty(fileDto.getPath())) {
            fileDto.setPath(fileDomain + fileDto.getPath());
            return fileDto;
        }

        String chunkFolderPath = getChunkFolderPath(filePath, key);
        int chunk = 1;
        // 判断第一分批是否存在
        String fileChuck = chunkFolderPath + chunk;
        java.io.File file = new java.io.File(fileChuck);
        // 不存在，直接返回
        if (!file.exists()) {
            return null;
        }
        // 判断后续分块是否存在
        chunk++;
        while (true) {
            fileChuck = chunkFolderPath + chunk;
            file = new java.io.File(fileChuck);
            if (!file.exists()) {
                break;
            }
            chunk++;
        }
        // 返回最后一块分块号
        fileDto.setShardIndex(chunk - 1);
        return fileDto;
    }

    // 获取分片存储文件夹路径
    private String getChunkFolderPath(String filePath, String key) throws FileNotFoundException {
        String prefix = ResourceUtils.getURL("classpath:").getPath() + filePath;
        prefix += key.charAt(0) + "/" + key.charAt(1) + "/" + key + "/chunk/";
        return prefix;
    }

    // 获取合并文件夹路径
    private String getFilePath(String filePath, String key) throws FileNotFoundException {
        String prefix = ResourceUtils.getURL("classpath:").getPath() + filePath;
        prefix += key.charAt(0) + "/" + key.charAt(1) + "/" + key + "/";
        return prefix;
    }

    // 获取文件相对路径
    private String getFileRelativePath(String key, String ext) {
        return key.charAt(0) + "/" + key.charAt(1) + "/" + key + "/" + key + "." + ext;
    }

    /**
     * 列表查询
     */
    public void list(PageDto pageDto) {
        PageHelper.startPage(pageDto.getPage(), pageDto.getSize());
        FileExample fileExample = new FileExample();
        List<File> fileList = fileMapper.selectByExample(fileExample);
        PageInfo<File> pageInfo = new PageInfo<>(fileList);
        pageDto.setTotal(pageInfo.getTotal());
        List<FileDto> fileDtoList = CopyUtil.copyList(fileList, FileDto.class);
        pageDto.setList(fileDtoList);
    }

    public void save(FileDto fileDto) {

        File file = CopyUtil.copy(fileDto, File.class);
        //根据key值去数据库查询File
        File fileDb = selectByKey(fileDto.getKey());
        //判断是新增 还是修改
        if (fileDb == null) {
            this.insert(file);
        } else {
            //如果是更新的话 先更改文件的ShardIndex属性 在update
            fileDb.setShardIndex(fileDto.getShardIndex());
            this.update(fileDb);
        }

    }

    //新增数据
    private void insert(File file) {

        Date now = new Date();
        file.setCreatedAt(now);
        file.setUpdatedAt(now);

        file.setId(UuidUtil.getShortUuid());
        fileMapper.insert(file);
    }

    //更新数据
    private void update(File file) {
        file.setUpdatedAt(new Date());
        fileMapper.updateByPrimaryKey(file);
    }

    public void delete(String id) {

        fileMapper.deleteByPrimaryKey(id);
    }

    //根据前端生成的MD5标识查询文件
    public File selectByKey(String key) {
        FileExample example = new FileExample();
        example.createCriteria().andKeyEqualTo(key);
        List<File> fileList = fileMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(fileList)) {
            return null;
        } else {
            return fileList.get(0);
        }
    }

    /**
     * 根据文件标识查询数据库记录
     */
    public FileDto findByKey(String key) {
        return CopyUtil.copy(selectByKey(key), FileDto.class);
    }

}//end class
