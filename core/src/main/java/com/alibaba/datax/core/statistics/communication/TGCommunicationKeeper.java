package com.alibaba.datax.core.statistics.communication;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yunzhong
 *
 */
public abstract class TGCommunicationKeeper {

	public abstract void registerTaskGroupCommunication(Long jobId, Integer taskGroupId, Communication communication);

	public abstract List<Communication> getAll(Long jobId);

	protected abstract Set<Integer> tgIdSet(Long jobId);

	protected abstract Communication get(Long jobId, int taskGroupId);

	public abstract boolean containsTGId(Long jobId, int taskGroupId);

	public abstract void delete(Long jobId);

	public abstract Map<Integer, Communication> getTaskGroupCommunicationMap(Long jobId);

	public abstract void update(Long jobId, int taskGroupId, Communication communication);

}
