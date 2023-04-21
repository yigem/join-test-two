package com.douding.business.controller.admin;


import com.douding.server.dto.PageDto;
import com.douding.server.dto.ResponseDto;
import com.douding.server.dto.TeacherDto;
import com.douding.server.service.TeacherService;
import com.douding.server.util.ValidatorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/admin/teacher")
public class TeacherController {

    private static final Logger LOG = LoggerFactory.getLogger(TeacherController.class);
    //给了日志用的
    public static final String BUSINESS_NAME = "讲师";

    @Resource
    private TeacherService teacherService;

    /**
     * 分页查询讲师信息
     *
     * @param pageDto 请求分页对象
     * @return 统一响应讲师分页信息
     */
    @RequestMapping("/list")
    public ResponseDto<PageDto<TeacherDto>> list(PageDto<TeacherDto> pageDto) {
        ResponseDto<PageDto<TeacherDto>> responseDto = new ResponseDto<>();
        teacherService.list(pageDto);
        responseDto.setContent(pageDto);
        return responseDto;
    }

    /**
     * 新增或更新讲师信息
     *
     * @param teacherDto 讲师数据对象
     * @return 讲师对象
     */
    @PostMapping("/save")
    public ResponseDto<TeacherDto> save(@RequestBody TeacherDto teacherDto) {
        ValidatorUtil.require(teacherDto.getName(), "姓名");
        ValidatorUtil.require(teacherDto.getNickname(), "昵称");
        ValidatorUtil.require(teacherDto.getPosition(), "职位");
        ValidatorUtil.require(teacherDto.getMotto(), "座右铭");
        ValidatorUtil.require(teacherDto.getIntro(), "简介");
        ResponseDto<TeacherDto> responseDto = new ResponseDto<>();
        teacherService.save(teacherDto);
        responseDto.setContent(teacherDto);
        return responseDto;
    }

    /**
     * 根据 id 删除讲师
     *
     * @param id 讲师 id
     * @return 返回统一结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseDto<TeacherDto> delete(@PathVariable String id) {
        ResponseDto<TeacherDto> responseDto = new ResponseDto<>();
        teacherService.delete(id);
        return responseDto;
    }

    /**
     * 获取所有讲师信息
     *
     * @return 所有讲师列表
     */
    @RequestMapping("/all")
    public ResponseDto<List<TeacherDto>> all() {
        ResponseDto<List<TeacherDto>> responseDto = new ResponseDto<>();
        List<TeacherDto> all = teacherService.all();
        responseDto.setContent(all);
        return responseDto;
    }

}//end class
