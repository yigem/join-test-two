package com.douding.server.service;

import com.douding.server.domain.Section;
import com.douding.server.domain.SectionExample;
import com.douding.server.dto.SectionDto;
import com.douding.server.dto.SectionPageDto;
import com.douding.server.mapper.SectionMapper;
import com.douding.server.util.CopyUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;


@Service
public class SectionService {

    @Resource
    private SectionMapper sectionMapper;

    @Resource
    private CourseService courseService;


    /**
     * 列表查询
     */
    public void list(SectionPageDto sectionPageDto) {
        PageHelper.startPage(sectionPageDto.getPage(), sectionPageDto.getSize());
        SectionExample sectionExample = new SectionExample();
        if (!StringUtils.isEmpty(sectionPageDto.getCourseId())) {
            SectionExample.Criteria criteria = sectionExample.createCriteria();
            criteria.andCourseIdEqualTo(sectionPageDto.getCourseId());
        }
        if (!StringUtils.isEmpty(sectionPageDto.getChapterId())) {
            SectionExample.Criteria criteria = sectionExample.createCriteria();
            criteria.andChapterIdEqualTo(sectionPageDto.getChapterId());
        }
        sectionExample.setOrderByClause("sort asc");
        List<Section> sections = sectionMapper.selectByExample(sectionExample);
        PageInfo<Section> pageInfo = new PageInfo<>(sections);
        sectionPageDto.setTotal(pageInfo.getTotal());
        List<SectionDto> sectionDtoList = CopyUtil.copyList(sections, SectionDto.class);
        sectionPageDto.setList(sectionDtoList);
    }


    public void save(SectionDto sectionDto) {

    }

    //新增数据
    private void insert(Section section) {


    }

    //更新数据
    private void update(Section section) {

    }

    public void delete(String id) {

    }

    /**
     * 查询某一课程下的所有节
     */
    public List<SectionDto> listByCourse(String courseId) {
        SectionExample sectionExample = new SectionExample();
        SectionExample.Criteria criteria = sectionExample.createCriteria();
        criteria.andCourseIdEqualTo(courseId);
        List<Section> sections = sectionMapper.selectByExample(sectionExample);
        return CopyUtil.copyList(sections, SectionDto.class);
    }

}//end class
