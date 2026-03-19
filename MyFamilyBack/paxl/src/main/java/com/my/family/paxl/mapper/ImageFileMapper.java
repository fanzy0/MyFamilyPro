package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.ImageFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 图片文件表数据访问层
 * 数据库表：t_image_file
 *
 * @author fan
 * @date 2026/03/17
 */
@Mapper
public interface ImageFileMapper extends BaseMapper<ImageFileDO> {

    int insert(ImageFileDO imageFileDO);

    /**
     * 查询图片二进制数据
     *
     * @param id 主键ID
     * @return 图片文件数据（仅包含 fileData / fileSize），不存在返回null
     */
    ImageFileDO selectDataById(@Param("id") Long id);

    /**
     * 查询图片元信息（不含大字段）
     *
     * @param id 主键ID
     * @return 图片文件数据（包含 contentType 等），不存在返回null
     */
    ImageFileDO selectMetaById(@Param("id") Long id);
}

