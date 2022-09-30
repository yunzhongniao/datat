package com.alibaba.datax.core.statistics.communication;

/**
 * @author yunzhong
 *
 */
public abstract class JobCommunicationKeeper {

	public abstract void reportJobCommunication(Long jobId, Communication communication);


}
