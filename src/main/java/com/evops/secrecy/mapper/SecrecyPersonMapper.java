package com.evops.secrecy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evops.secrecy.entity.SecrecyPerson;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecrecyPersonMapper extends BaseMapper<SecrecyPerson> {
}
