package com.yunzhong.datat.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.datax.core.EngineDatat;
import com.alibaba.datax.dataxservice.face.domain.enums.State;
import com.yunzhong.datat.application.mapper.JobContentMapper;
import com.yunzhong.datat.application.mapper.JobInstanceMapper;
import com.yunzhong.datat.application.model.JobContent;
import com.yunzhong.datat.application.model.JobInstance;
import com.yunzhong.datat.application.service.JobService;

@Service
public class JobServiceImpl implements JobService {
	private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

	@Value("${datat.application.server_id}")
	private String serverId;

	@Autowired
	private JobContentMapper jobContentMapper;

	@Autowired
	private JobInstanceMapper jobInstanceMapper;

	@Override
	public String run(String jobContent) {
		try {
			EngineDatat.entry(1L, jobContent);
			return "success";
		} catch (Throwable e) {
			log.error("Failed to run job.", e);
		}
		return "error";
	}

	@Override
	public String run(Long jobId) {
		JobContent selectByPrimaryKey = jobContentMapper.selectByPrimaryKey(jobId);
		if (selectByPrimaryKey != null) {
			JobInstance jobInstance = new JobInstance();
			try {
				jobInstance.setJobContentId(selectByPrimaryKey.getJobContentId());
				jobInstance.setServerId(serverId);
				jobInstance.setStartTimestamp(System.currentTimeMillis());
				jobInstance.setState(State.RUNNING.name());
				jobInstanceMapper.insertSelective(jobInstance);
				EngineDatat.entry(jobInstance.getId(), selectByPrimaryKey.getJobContent());

				jobInstance.setState(State.SUCCEEDED.name());
				jobInstance.setEndTimestamp(System.currentTimeMillis());
				jobInstanceMapper.updateByPrimaryKeySelective(jobInstance);
				return "success";
			} catch (Throwable e) {
				log.error("Failed to run job.", e);
				jobInstance.setState(State.FAILED.name());
				jobInstance.setEndTimestamp(System.currentTimeMillis());
				jobInstanceMapper.updateByPrimaryKeySelective(jobInstance);
			}
		}
		return "error";
	}

}
