package com.yunzhong.datat.application.service;

public interface JobService {

	String run(String jobContent);

	String run(Long jobId);

}
