package com.alibaba.datax.core.statistics.container.collector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.DistributeTGCommunicationManager;
import com.alibaba.datax.core.statistics.communication.LocalTGCommunicationManager;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.dataxservice.face.domain.enums.State;

public class ProcessDistributeCollector extends AbstractCollector {

	public ProcessDistributeCollector(Long jobId) {
		super.setJobId(jobId);
	}

	@Override
	public Communication collectFromTaskGroup() {
		return DistributeTGCommunicationManager.getJobCommunication(this.getJobId());
	}

	public Map<Integer, Communication> getTaskCommunicationMap() {
		return DistributeTGCommunicationManager.getTaskGroupCommunicationMap(getJobId());
	}

	public void registerTGCommunication(List<Configuration> taskGroupConfigurationList) {
		for (Configuration config : taskGroupConfigurationList) {
			int taskGroupId = config.getInt(CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_ID);
			DistributeTGCommunicationManager.registerTaskGroupCommunication(getJobId(), taskGroupId,
					new Communication());
		}
	}

	public void registerTaskCommunication(List<Configuration> taskConfigurationList) {
		for (Configuration taskConfig : taskConfigurationList) {
			int taskId = taskConfig.getInt(CoreConstant.TASK_ID);
			DistributeTGCommunicationManager.registerTaskGroupCommunication(getJobId(), taskId, new Communication());
		}
	}

	public Communication collectFromTask() {
		Communication communication = new Communication();
		communication.setState(State.SUCCEEDED);
		Collection<Communication> values = DistributeTGCommunicationManager.getTaskGroupCommunicationMap(getJobId())
				.values();
		for (Communication taskCommunication : values) {
			communication.mergeFrom(taskCommunication);
		}

		return communication;
	}

	public Map<Integer, Communication> getTGCommunicationMap() {
		return DistributeTGCommunicationManager.getTaskGroupCommunicationMap(getJobId());
	}

	public Communication getTGCommunication(Integer taskGroupId) {
		return DistributeTGCommunicationManager.getTaskGroupCommunication(getJobId(), taskGroupId);
	}

	public Communication getTaskCommunication(Integer taskId) {
		return DistributeTGCommunicationManager.getTaskGroupCommunication(getJobId(), taskId);
	}

}
