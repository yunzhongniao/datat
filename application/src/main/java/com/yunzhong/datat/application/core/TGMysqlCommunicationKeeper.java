package com.yunzhong.datat.application.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.TGCommunicationKeeper;
import com.alibaba.datax.dataxservice.face.domain.enums.State;
import com.alibaba.druid.support.json.JSONUtils;
import com.yunzhong.datat.application.common.JacksonUtil;
import com.yunzhong.datat.application.mapper.TgCommunicationMapper;
import com.yunzhong.datat.application.model.TgCommunication;
import com.yunzhong.datat.application.model.TgCommunicationExample;

/**
 * @author yunzhong
 *
 */
@Service
public class TGMysqlCommunicationKeeper extends TGCommunicationKeeper {

	@Autowired
	private TgCommunicationMapper tgCommunicationMapper;

	@Override
	public void registerTaskGroupCommunication(Long jobId, Integer taskGroupId, Communication communication) {
		TgCommunication tgCommunication = new TgCommunication();
		tgCommunication.setJobId(jobId);
		tgCommunication.setTgId(taskGroupId);
		if (communication.getCounter() != null) {
			tgCommunication.setTgCounter(JSONUtils.toJSONString(communication.getCounter()));
		}
		if (communication.getMessage() != null) {
			tgCommunication.setTgMessage(JSONUtils.toJSONString(communication.getMessage()));
		}
		if (communication.getState() != null) {
			tgCommunication.setTgState(communication.getState().name());
		}
		tgCommunication.setTimestamp(System.currentTimeMillis());
		tgCommunicationMapper.insertSelective(tgCommunication);
	}

	@Override
	public List<Communication> getAll(Long jobId) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId);
		List<TgCommunication> tgComms = tgCommunicationMapper.selectByExampleWithBLOBs(example);

		return tgComms.stream().map(tgComm -> {
			Communication comm = new Communication();
			comm.setState(State.valueOf(tgComm.getTgState()));
			comm.setTimestamp(tgComm.getTimestamp());
			if (StringUtils.hasLength(tgComm.getTgCounter())) {
				Map<String, Number> counterMap = JacksonUtil.string2Obj(tgComm.getTgCounter(), Map.class);
				comm.setCounter(counterMap);
			}
			return comm;
		}).collect(Collectors.toList());
	}

	@Override
	protected Set<Integer> tgIdSet(Long jobId) {
		return tgCommunicationMapper.selectByExample(new TgCommunicationExample()).stream()
				.map(TgCommunication::getTgId).collect(Collectors.toSet());
	}

	@Override
	protected Communication get(Long jobId, int taskGroupId) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId).andTgIdEqualTo(taskGroupId);
		List<TgCommunication> tgComms = tgCommunicationMapper.selectByExampleWithBLOBs(example);
		if (CollectionUtils.isEmpty(tgComms)) {
			return null;
		}
		TgCommunication tgComm = tgComms.get(0);
		Communication comm = new Communication();
		if (StringUtils.hasLength(tgComm.getTgState())) {
			comm.setState(State.valueOf(tgComm.getTgState()));
		}
		comm.setTimestamp(tgComm.getTimestamp());
		if (StringUtils.hasLength(tgComm.getTgCounter())) {
			Map<String, Number> counterMap = JacksonUtil.string2Obj(tgComm.getTgCounter(), Map.class);
			comm.setCounter(counterMap);
		}
		return comm;
	}

	@Override
	public boolean containsTGId(Long jobId, int taskGroupId) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId).andTgIdEqualTo(taskGroupId);
		long tgCommCount = tgCommunicationMapper.countByExample(example);
		return tgCommCount > 0;
	}

	@Override
	public void delete(Long jobId) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId);
		tgCommunicationMapper.deleteByExample(example);
	}

	@Override
	public Map<Integer, Communication> getTaskGroupCommunicationMap(Long jobId) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId);
		List<TgCommunication> tgComms = tgCommunicationMapper.selectByExampleWithBLOBs(example);

		return tgComms.stream().collect(Collectors.toMap(TgCommunication::getTgId, tgComm -> {
			Communication comm = new Communication();
			comm.setState(State.valueOf(tgComm.getTgState()));
			comm.setTimestamp(tgComm.getTimestamp());
			if (!StringUtils.hasLength(tgComm.getTgCounter())) {
				Map<String, Number> counterMap = JacksonUtil.string2Obj(tgComm.getTgCounter(), Map.class);
				comm.setCounter(counterMap);
			}
			return comm;
		}));
	}

	@Override
	public void update(Long jobId, int taskGroupId, Communication communication) {
		TgCommunicationExample example = new TgCommunicationExample();
		example.createCriteria().andJobIdEqualTo(jobId).andTgIdEqualTo(taskGroupId);
		List<TgCommunication> tgComms = tgCommunicationMapper.selectByExampleWithBLOBs(example);
		if (CollectionUtils.isEmpty(tgComms)) {
			return;
		}
		TgCommunication tgCommunication = tgComms.get(0);
		tgCommunication.setJobId(jobId);
		tgCommunication.setTgId(taskGroupId);
		if (communication.getCounter() != null) {
			tgCommunication.setTgCounter(JSONUtils.toJSONString(communication.getCounter()));
		}
		if (communication.getMessage() != null) {
			tgCommunication.setTgMessage(JSONUtils.toJSONString(communication.getMessage()));
		}
		tgCommunication.setTgState(communication.getState().name());
		tgCommunication.setTimestamp(System.currentTimeMillis());
		tgCommunicationMapper.updateByPrimaryKey(tgCommunication);
	}

}
