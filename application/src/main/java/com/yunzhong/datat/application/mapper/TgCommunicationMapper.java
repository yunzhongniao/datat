package com.yunzhong.datat.application.mapper;

import com.yunzhong.datat.application.model.TgCommunication;
import com.yunzhong.datat.application.model.TgCommunicationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TgCommunicationMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    long countByExample(TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int deleteByExample(TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int deleteByPrimaryKey(@Param("jobId") Long jobId, @Param("tgId") Integer tgId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int insert(TgCommunication row);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int insertSelective(TgCommunication row);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    List<TgCommunication> selectByExampleWithBLOBs(TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    List<TgCommunication> selectByExample(TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    TgCommunication selectByPrimaryKey(@Param("jobId") Long jobId, @Param("tgId") Integer tgId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByExampleSelective(@Param("row") TgCommunication row, @Param("example") TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByExampleWithBLOBs(@Param("row") TgCommunication row, @Param("example") TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByExample(@Param("row") TgCommunication row, @Param("example") TgCommunicationExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByPrimaryKeySelective(TgCommunication row);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByPrimaryKeyWithBLOBs(TgCommunication row);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tg_communication
     *
     * @mbg.generated Fri Sep 30 08:52:13 CST 2022
     */
    int updateByPrimaryKey(TgCommunication row);
}