package com.douding.server.service;

import com.douding.server.domain.Course;
import com.douding.server.domain.CourseContent;
import com.douding.server.domain.CourseExample;
import com.douding.server.dto.*;
import com.douding.server.enums.CourseStatusEnum;
import com.douding.server.mapper.CourseContentMapper;
import com.douding.server.mapper.CourseMapper;
import com.douding.server.mapper.my.MyCourseMapper;
import com.douding.server.util.CopyUtil;
import com.douding.server.util.UuidUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class CourseService {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private MyCourseMapper myCourseMapper;

    @Resource
    private CourseCategoryService myCategoryService;

    @Resource
    private CourseContentMapper courseContentMapper;

    @Resource
    private TeacherService teacherService;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;

    @Resource
    private CategoryService categoryService;


    private static final Logger LOG = LoggerFactory.getLogger(CourseService.class);


    /**
     * 列表查询：关联课程分类表 web接口
     *
     * @param pageDto
     */
    public void list(CoursePageDto pageDto) {
        // 分页查询课程基本信息
        PageHelper.startPage(pageDto.getPage(), pageDto.getSize());
        List<CourseDto> courses = myCourseMapper.list(pageDto);
        PageInfo<CourseDto> pageInfo = new PageInfo<>(courses);
        pageDto.setTotal(pageInfo.getTotal());

        courses.forEach(this::findCourseDetails);

        pageDto.setList(courses);
    }

    private void findCourseDetails(CourseDto courseDto) {
        // 查询讲师信息
        TeacherDto teacherDto = teacherService.findById(courseDto.getTeacherId());
        courseDto.setTeacher(teacherDto);
        // 查询课程内容
        CourseContent courseContent = courseContentMapper.selectByPrimaryKey(courseDto.getId());
        if (courseContent != null) {
            courseDto.setContent(courseContent.getContent());
        }
        // 查询分类信息
        List<CourseCategoryDto> courseCategoryDtoList = myCategoryService.listByCourse(courseDto.getId());
        List<CategoryDto> categoryDtoList = courseCategoryDtoList.stream()
                .map(courseCategoryDto -> categoryService.findById(courseCategoryDto.getCategoryId()))
                .collect(Collectors.toList());
        courseDto.setCategorys(categoryDtoList);
        // 查询章节信息
        List<ChapterDto> chapterDtos = chapterService.listByCourse(courseDto.getId());
        courseDto.setChapters(chapterDtos);
        // 查询小节信息
        List<SectionDto> sectionDtos = sectionService.listByCourse(courseDto.getId());
        courseDto.setSections(sectionDtos);
    }


    @Transactional
    public void save(CourseDto courseDto) {
        Course course = CopyUtil.copy(courseDto, Course.class);

        if (StringUtils.isEmpty(courseDto.getId())) {
            this.insert(course);
            SortDto sortDto = new SortDto();
            sortDto.setId(course.getId());
            sortDto.setNewSort(1);
            sortDto.setOldSort(course.getSort());
            this.sort(sortDto);
        } else {
            this.update(course);
        }
        myCategoryService.saveBatch(course.getId(), courseDto.getCategorys());
    }

    //新增数据
    private void insert(Course course) {
        course.setId(UuidUtil.getShortUuid());
        Date now = new Date();
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        courseMapper.insert(course);
    }

    //更新数据
    private void update(Course course) {
        Date now = new Date();
        course.setUpdatedAt(now);
        courseMapper.updateByPrimaryKey(course);
    }

    public void delete(String id) {
        courseMapper.deleteByPrimaryKey(id);
    }

    //更新课程时长
    public void updateTime(@Param("courseId") String courseId) {

    }

    //课程内容相关的操作 查找 新增,修改
    public CourseContentDto findContent(String id) {
        CourseContent courseContent = courseContentMapper.selectByPrimaryKey(id);
        return CopyUtil.copy(courseContent, CourseContentDto.class);
    }

    //新增内容 或者修改内容
    public int saveContent(CourseContentDto contentDto) {
        CourseContent content = CopyUtil.copy(contentDto, CourseContent.class);
        return courseContentMapper.updateByPrimaryKeySelective(content);
    }


    @Transactional
    public void sort(SortDto sortDto) {
        myCourseMapper.updateSort(sortDto);
        myCourseMapper.moveSortBackward(sortDto);
    }

    /**
     * 查找某一课程，供web模块用，只能查已发布的
     *
     * @param id
     * @return
     */
    public CourseDto findCourse(String id) {
        CourseExample courseExample = new CourseExample();
        CourseExample.Criteria criteria = courseExample.createCriteria();
        criteria.andIdEqualTo(id);
        criteria.andStatusEqualTo(CourseStatusEnum.PUBLISH.getDesc());
        List<Course> courses = courseMapper.selectByExample(courseExample);
        if (courses == null || courses.size() == 0) {
            return null;
        }
        CourseDto courseDto = CopyUtil.copy(courses.get(0), CourseDto.class);
        this.findCourseDetails(courseDto);
        return courseDto;
    }

    /**
     * 新课列表查询，只查询已发布的，按创建日期倒序
     */
    public List<CourseDto> listNew(PageDto pageDto) {
        PageHelper.startPage(pageDto.getPage(), pageDto.getSize());
        CourseExample courseExample = new CourseExample();
        CourseExample.Criteria criteria = courseExample.createCriteria();
        criteria.andStatusEqualTo(CourseStatusEnum.PUBLISH.getCode());
        courseExample.setOrderByClause("created_at desc");
        List<Course> courses = courseMapper.selectByExample(courseExample);
        return CopyUtil.copyList(courses, CourseDto.class);
    }


}//end class
