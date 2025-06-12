package com.comerzzia.brico.pos.selfcheckout.persistence.motivos;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface MotivoMapper {
    long countByExample(MotivoExample example);

    int deleteByExample(MotivoExample example);

    int deleteByPrimaryKey(MotivoKey key);

    int insert(Motivo row);

    int insertSelective(Motivo row);

    List<Motivo> selectByExampleWithRowbounds(MotivoExample example, RowBounds rowBounds);

    List<Motivo> selectByExample(MotivoExample example);

    Motivo selectByPrimaryKey(MotivoKey key);

    int updateByExampleSelective(@Param("row") Motivo row, @Param("example") MotivoExample example);

    int updateByExample(@Param("row") Motivo row, @Param("example") MotivoExample example);

    int updateByPrimaryKeySelective(Motivo row);

    int updateByPrimaryKey(Motivo row);
}