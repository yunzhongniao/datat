package com.alibaba.datax.core.statistics.container.report;

import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.DistributeTGCommunicationManager;

public class ProcessDistributeReporter extends AbstractReporter {
	private Long jobId;
	
	public ProcessDistributeReporter(Long jobId) {
		this.jobId = jobId;
	}

	@Override
	public void reportJobCommunication(Long jobId, Communication communication) {
		DistributeTGCommunicationManager.reportJobCommunication(jobId, communication);
	}

	@Override
	public void reportTGCommunication(Integer taskGroupId, Communication communication) {
		DistributeTGCommunicationManager.updateTaskGroupCommunication(jobId, taskGroupId, communication);
	}
}