package com.yunzhong.datat.application.core;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.datax.core.statistics.communication.DistributeTGCommunicationManager;

@Component
public class EnvInitialization {

	@Autowired
	private TGMysqlCommunicationKeeper tgKeeper;

	@Autowired
	private JobMysqlCommunicationKeeper jobKeeper;

	@PostConstruct
	public void envInit() {
		// 初始化job，taskgroup通信的mysql实现
		DistributeTGCommunicationManager.registerKeeper(tgKeeper, jobKeeper);
	}
}
