package com.yunzhong.datat.application.core;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.JobCommunicationKeeper;
import com.alibaba.fastjson.JSON;
import com.yunzhong.datat.application.mapper.JobCommunicationMapper;
import com.yunzhong.datat.application.mapper.JobInstanceMapper;
import com.yunzhong.datat.application.model.JobCommunication;
import com.yunzhong.datat.application.model.JobInstance;
import com.yunzhong.datat.application.model.JobInstanceExample;

/**
 * @author yunzhong
 *
 */
@Service
public class JobMysqlCommunicationKeeper extends JobCommunicationKeeper {

	@Autowired
	private JobCommunicationMapper jobCommunicationMapper;

	@Autowired
	private JobInstanceMapper jobInstanceMapper;

	@Override
	public void reportJobCommunication(Long jobId, Communication communication) {
		String counter = JSON.toJSONString(communication.getCounter());
		String jobState = communication.getState().name();
		String message = JSON.toJSONString(communication.getMessage());
		long timestamp = communication.getTimestamp();
		JobCommunication jobComm = new JobCommunication();
		jobComm.setCounter(counter);
		jobComm.setInstanceId(jobId);
		JobInstanceExample jobInstanceExample = new JobInstanceExample();
		jobInstanceExample.createCriteria().andIdEqualTo(jobId);
		List<JobInstance> jobInstances = jobInstanceMapper.selectByExample(jobInstanceExample);
		if (CollectionUtils.isNotEmpty(jobInstances)) {
			jobComm.setJobContentId(jobInstances.get(0).getJobContentId());
		}
		jobComm.setJobState(jobState);
		jobComm.setMessage(message);
		jobComm.setTimestamp(timestamp);

		jobCommunicationMapper.insertSelective(jobComm);
	}

}
