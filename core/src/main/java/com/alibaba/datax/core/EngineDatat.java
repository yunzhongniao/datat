package com.alibaba.datax.core;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.element.ColumnCast;
import com.alibaba.datax.common.statistics.PerfTrace;
import com.alibaba.datax.common.statistics.VMInfo;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.job.JobDistributeContainer;
import com.alibaba.datax.core.taskgroup.TaskGroupContainer;
import com.alibaba.datax.core.util.ConfigParser;
import com.alibaba.datax.core.util.ConfigurationValidate;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.core.util.container.LoadUtil;

/**
 * Engine是DataX入口类，该类负责初始化Job或者Task的运行容器，并运行插件的Job或者Task逻辑<br>
 * 复写Engine，支持多启动
 * 
 * @author yunzhong
 *
 */
public class EngineDatat {
	private static final Logger LOG = LoggerFactory.getLogger(EngineDatat.class);

	private static String RUNTIME_MODE;

	/* check job model (job/task) first */
	public void start(Configuration allConf) {

		// 绑定column转换信息
		ColumnCast.bind(allConf);

		/**
		 * 初始化PluginLoader，可以获取各种插件配置
		 */
		LoadUtil.bind(allConf);

		boolean isJob = !("taskGroup".equalsIgnoreCase(allConf.getString(CoreConstant.DATAX_CORE_CONTAINER_MODEL)));
		// JobContainer会在schedule后再行进行设置和调整值
		int channelNumber = 0;
		AbstractContainer container;
		long instanceId;
		int taskGroupId = -1;
		if (isJob) {
			allConf.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_MODE, RUNTIME_MODE);
			container = new JobDistributeContainer(allConf);
			instanceId = allConf.getLong(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, 0);

		} else {
			container = new TaskGroupContainer(allConf);
			instanceId = allConf.getLong(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID);
			taskGroupId = allConf.getInt(CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_ID);
			channelNumber = allConf.getInt(CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_CHANNEL);
		}

		// 缺省打开perfTrace
		boolean traceEnable = allConf.getBool(CoreConstant.DATAX_CORE_CONTAINER_TRACE_ENABLE, true);
		boolean perfReportEnable = allConf.getBool(CoreConstant.DATAX_CORE_REPORT_DATAX_PERFLOG, true);

		// standalone模式的 datax shell任务不进行汇报
		if (instanceId == -1) {
			perfReportEnable = false;
		}

		int priority = 0;
		try {
			priority = Integer.parseInt(System.getenv("SKYNET_PRIORITY"));
		} catch (NumberFormatException e) {
			LOG.warn("prioriy set to 0, because NumberFormatException, the value is: " + System.getProperty("PROIORY"));
		}

		Configuration jobInfoConfig = allConf.getConfiguration(CoreConstant.DATAX_JOB_JOBINFO);
		// 初始化PerfTrace
		PerfTrace perfTrace = PerfTrace.getInstance(isJob, instanceId, taskGroupId, priority, traceEnable);
		perfTrace.setJobInfo(jobInfoConfig, perfReportEnable, channelNumber);
		container.start();

	}

	// 注意屏蔽敏感信息
	public static String filterJobConfiguration(final Configuration configuration) {
		Configuration jobConfWithSetting = configuration.getConfiguration("job").clone();

		Configuration jobContent = jobConfWithSetting.getConfiguration("content");

		filterSensitiveConfiguration(jobContent);

		jobConfWithSetting.set("content", jobContent);

		return jobConfWithSetting.beautify();
	}

	public static Configuration filterSensitiveConfiguration(Configuration configuration) {
		Set<String> keys = configuration.getKeys();
		for (final String key : keys) {
			boolean isSensitive = StringUtils.endsWithIgnoreCase(key, "password")
					|| StringUtils.endsWithIgnoreCase(key, "accessKey");
			if (isSensitive && configuration.get(key) instanceof String) {
				configuration.set(key, configuration.getString(key).replaceAll(".", "*"));
			}
		}
		return configuration;
	}

	public static void entry(Long jobId, String jobContent) throws Throwable {

		Configuration configuration = ConfigParser.parse2(jobContent);

		configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, jobId);

		// 打印vmInfo
		VMInfo vmInfo = VMInfo.getVmInfo();
		if (vmInfo != null) {
			LOG.info(vmInfo.toString());
		}

		LOG.info("\n" + EngineDatat.filterJobConfiguration(configuration) + "\n");

		LOG.debug(configuration.toJSON());

		ConfigurationValidate.doValidate(configuration);
		EngineDatat engine = new EngineDatat();
		engine.start(configuration);
	}

}
