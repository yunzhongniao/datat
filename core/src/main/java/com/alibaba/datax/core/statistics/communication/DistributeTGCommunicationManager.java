package com.alibaba.datax.core.statistics.communication;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.alibaba.datax.dataxservice.face.domain.enums.State;

public final class DistributeTGCommunicationManager {

	private static TGCommunicationKeeper tgCommunicationKeeper = null;

	public static void registerKeeper(TGCommunicationKeeper keeper) {
		DistributeTGCommunicationManager.tgCommunicationKeeper = keeper;
	}

	public static void registerTaskGroupCommunication(Long jobId, int taskGroupId, Communication communication) {
		tgCommunicationKeeper.registerTaskGroupCommunication(jobId, taskGroupId, communication);
	}

	public static Communication getJobCommunication(Long jobId) {
		Communication communication = new Communication();
		communication.setState(State.SUCCEEDED);

		for (Communication taskGroupCommunication : tgCommunicationKeeper.getAll(jobId)) {
			communication.mergeFrom(taskGroupCommunication);
		}

		return communication;
	}

	/**
	 * 采用获取taskGroupId后再获取对应communication的方式， 防止map遍历时修改，同时也防止对map key-value对的修改
	 *
	 * @return
	 */
	public static Set<Integer> getTaskGroupIdSet(Long jobId) {
		return tgCommunicationKeeper.tgIdSet(jobId);
	}

	public static Communication getTaskGroupCommunication(Long jobId, int taskGroupId) {
		return tgCommunicationKeeper.get(jobId, taskGroupId);
	}

	public static void updateTaskGroupCommunication(Long jobId, final int taskGroupId, final Communication communication) {
		Validate.isTrue(tgCommunicationKeeper.containsTGId(jobId, taskGroupId), String.format(
				"taskGroupCommunicationMap中没有注册taskGroupId[%d]的Communication，" + "无法更新该taskGroup的信息", taskGroupId));
		tgCommunicationKeeper.update(jobId, taskGroupId, communication);
	}

	public static void clear(Long jobId) {
		tgCommunicationKeeper.delete(jobId);
	}

	public static Map<Integer, Communication> getTaskGroupCommunicationMap(Long jobId) {
		return tgCommunicationKeeper.getTaskGroupCommunicationMap(jobId);
	}
}