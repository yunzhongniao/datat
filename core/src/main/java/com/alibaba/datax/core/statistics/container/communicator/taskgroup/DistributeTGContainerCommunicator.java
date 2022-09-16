package com.alibaba.datax.core.statistics.container.communicator.taskgroup;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.container.report.ProcessDistributeReporter;

public class DistributeTGContainerCommunicator extends AbstractTGContainerCommunicator {

	public DistributeTGContainerCommunicator(Configuration configuration) {
		super(configuration);
		super.setReporter(new ProcessDistributeReporter(getJobId()));
	}

	@Override
	public void report(Communication communication) {
		super.getReporter().reportTGCommunication(super.taskGroupId, communication);
	}

}
