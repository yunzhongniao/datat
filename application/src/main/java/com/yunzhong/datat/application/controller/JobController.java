package com.yunzhong.datat.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunzhong.datat.application.common.CommonResponse;
import com.yunzhong.datat.application.service.JobService;
import com.yunzhong.datat.application.vo.JobRunVO;

@RestController
@RequestMapping("/job")
public class JobController {

	@Autowired
	private JobService jobService;

	@PostMapping("/run/temp")
	public CommonResponse runTemp(@RequestBody JobRunVO job) {
		return CommonResponse.success(jobService.run(job.getJobContent()));
	}

	@PutMapping("/run/{jobId}")
	public CommonResponse runTemp(@PathVariable Long jobId) {
		return CommonResponse.success(jobService.run(jobId));
	}

	@PostMapping("/create")
	public CommonResponse createJob() {
		return CommonResponse.success(null);
	}
}
