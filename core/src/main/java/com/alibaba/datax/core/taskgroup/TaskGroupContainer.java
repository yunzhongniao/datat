package com.alibaba.datax.core.taskgroup;

import com.alibaba.datax.common.constant.PluginType;
import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.statistics.PerfRecord;
import com.alibaba.datax.common.statistics.PerfTrace;
import com.alibaba.datax.common.statistics.VMInfo;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.AbstractContainer;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.CommunicationTool;
import com.alibaba.datax.core.statistics.container.communicator.taskgroup.DistributeTGContainerCommunicator;
import com.alibaba.datax.core.statistics.container.communicator.taskgroup.StandaloneTGContainerCommunicator;
import com.alibaba.datax.core.statistics.plugin.task.AbstractTaskPluginCollector;
import com.alibaba.datax.core.taskgroup.runner.AbstractRunner;
import com.alibaba.datax.core.taskgroup.runner.ReaderRunner;
import com.alibaba.datax.core.taskgroup.runner.WriterRunner;
import com.alibaba.datax.core.transport.channel.Channel;
import com.alibaba.datax.core.transport.exchanger.BufferedRecordExchanger;
import com.alibaba.datax.core.transport.exchanger.BufferedRecordTransformerExchanger;
import com.alibaba.datax.core.transport.transformer.TransformerExecution;
import com.alibaba.datax.core.util.ClassUtil;
import com.alibaba.datax.core.util.FrameworkErrorCode;
import com.alibaba.datax.core.util.TransformerUtil;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.core.util.container.LoadUtil;
import com.alibaba.datax.dataxservice.face.domain.enums.State;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TaskGroupContainer extends AbstractContainer {
    private static final Logger LOG = LoggerFactory
            .getLogger(TaskGroupContainer.class);

    /**
     * ??????taskGroup??????jobId
     */
    private long jobId;

    /**
     * ??????taskGroupId
     */
    private int taskGroupId;

    /**
     * ?????????channel???
     */
    private String channelClazz;

    /**
     * task?????????????????????
     */
    private String taskCollectorClass;

    private TaskMonitor taskMonitor = TaskMonitor.getInstance();

    public TaskGroupContainer(Configuration configuration) {
        super(configuration);

        initCommunicator(configuration);

        this.jobId = this.configuration.getLong(
                CoreConstant.DATAX_CORE_CONTAINER_JOB_ID);
        this.taskGroupId = this.configuration.getInt(
                CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_ID);

        this.channelClazz = this.configuration.getString(
                CoreConstant.DATAX_CORE_TRANSPORT_CHANNEL_CLASS);
        this.taskCollectorClass = this.configuration.getString(
                CoreConstant.DATAX_CORE_STATISTICS_COLLECTOR_PLUGIN_TASKCLASS);
    }

    private void initCommunicator(Configuration configuration) {
        super.setContainerCommunicator(new DistributeTGContainerCommunicator(configuration));

    }

    public long getJobId() {
        return jobId;
    }

    public int getTaskGroupId() {
        return taskGroupId;
    }

    @Override
    public void start() {
        try {
            /**
             * ??????check????????????????????????????????????????????????????????????channel???
             */
            int sleepIntervalInMillSec = this.configuration.getInt(
                    CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_SLEEPINTERVAL, 100);
            /**
             * ??????????????????????????????????????????????????????
             */
            long reportIntervalInMillSec = this.configuration.getLong(
                    CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_REPORTINTERVAL,
                    10000);
            /**
             * 2??????????????????????????????
             */

            // ??????channel??????
            int channelNumber = this.configuration.getInt(
                    CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_CHANNEL);

            int taskMaxRetryTimes = this.configuration.getInt(
                    CoreConstant.DATAX_CORE_CONTAINER_TASK_FAILOVER_MAXRETRYTIMES, 1);

            long taskRetryIntervalInMsec = this.configuration.getLong(
                    CoreConstant.DATAX_CORE_CONTAINER_TASK_FAILOVER_RETRYINTERVALINMSEC, 10000);

            long taskMaxWaitInMsec = this.configuration.getLong(CoreConstant.DATAX_CORE_CONTAINER_TASK_FAILOVER_MAXWAITINMSEC, 60000);
            
            List<Configuration> taskConfigs = this.configuration
                    .getListConfiguration(CoreConstant.DATAX_JOB_CONTENT);

            if(LOG.isDebugEnabled()) {
                LOG.debug("taskGroup[{}]'s task configs[{}]", this.taskGroupId,
                        JSON.toJSONString(taskConfigs));
            }
            
            int taskCountInThisTaskGroup = taskConfigs.size();
            LOG.info(String.format(
                    "taskGroupId=[%d] start [%d] channels for [%d] tasks.",
                    this.taskGroupId, channelNumber, taskCountInThisTaskGroup));
            
            this.containerCommunicator.registerCommunication(taskConfigs);

            Map<Integer, Configuration> taskConfigMap = buildTaskConfigMap(taskConfigs); //taskId???task??????
            List<Configuration> taskQueue = buildRemainTasks(taskConfigs); //?????????task??????
            Map<Integer, TaskExecutor> taskFailedExecutorMap = new HashMap<Integer, TaskExecutor>(); //taskId?????????????????????
            List<TaskExecutor> runTasks = new ArrayList<TaskExecutor>(channelNumber); //????????????task
            Map<Integer, Long> taskStartTimeMap = new HashMap<Integer, Long>(); //??????????????????

            long lastReportTimeStamp = 0;
            Communication lastTaskGroupContainerCommunication = new Communication();

            while (true) {
            	//1.??????task??????
            	boolean failedOrKilled = false;
            	Map<Integer, Communication> communicationMap = containerCommunicator.getCommunicationMap();
            	for(Map.Entry<Integer, Communication> entry : communicationMap.entrySet()){
            		Integer taskId = entry.getKey();
            		Communication taskCommunication = entry.getValue();
                    if(!taskCommunication.isFinished()){
                        continue;
                    }
                    TaskExecutor taskExecutor = removeTask(runTasks, taskId);

                    //?????????runTasks??????????????????????????????monitor?????????
                    taskMonitor.removeTask(taskId);

                    //????????????task????????????failover????????????????????????????????????
            		if(taskCommunication.getState() == State.FAILED){
                        taskFailedExecutorMap.put(taskId, taskExecutor);
            			if(taskExecutor.supportFailOver() && taskExecutor.getAttemptCount() < taskMaxRetryTimes){
                            taskExecutor.shutdown(); //????????????executor
                            containerCommunicator.resetCommunication(taskId); //???task???????????????
            				Configuration taskConfig = taskConfigMap.get(taskId);
            				taskQueue.add(taskConfig); //????????????????????????
            			}else{
            				failedOrKilled = true;
                			break;
            			}
            		}else if(taskCommunication.getState() == State.KILLED){
            			failedOrKilled = true;
            			break;
            		}else if(taskCommunication.getState() == State.SUCCEEDED){
                        Long taskStartTime = taskStartTimeMap.get(taskId);
                        if(taskStartTime != null){
                            Long usedTime = System.currentTimeMillis() - taskStartTime;
                            LOG.info("taskGroup[{}] taskId[{}] is successed, used[{}]ms",
                                    this.taskGroupId, taskId, usedTime);
                            //usedTime*1000*1000 ?????????PerfRecord?????????ns?????????????????????????????????????????????????????????????????????????????????????????????
                            PerfRecord.addPerfRecord(taskGroupId, taskId, PerfRecord.PHASE.TASK_TOTAL,taskStartTime, usedTime * 1000L * 1000L);
                            taskStartTimeMap.remove(taskId);
                            taskConfigMap.remove(taskId);
                        }
                    }
            	}
            	
                // 2.?????????taskGroup???taskExecutor?????????????????????????????????
                if (failedOrKilled) {
                    lastTaskGroupContainerCommunication = reportTaskGroupCommunication(
                            lastTaskGroupContainerCommunication, taskCountInThisTaskGroup);

                    throw DataXException.asDataXException(
                            FrameworkErrorCode.PLUGIN_RUNTIME_ERROR, lastTaskGroupContainerCommunication.getThrowable());
                }
                
                //3.????????????????????????????????????????????????????????????????????????
                Iterator<Configuration> iterator = taskQueue.iterator();
                while(iterator.hasNext() && runTasks.size() < channelNumber){
                    Configuration taskConfig = iterator.next();
                    Integer taskId = taskConfig.getInt(CoreConstant.TASK_ID);
                    int attemptCount = 1;
                    TaskExecutor lastExecutor = taskFailedExecutorMap.get(taskId);
                    if(lastExecutor!=null){
                        attemptCount = lastExecutor.getAttemptCount() + 1;
                        long now = System.currentTimeMillis();
                        long failedTime = lastExecutor.getTimeStamp();
                        if(now - failedTime < taskRetryIntervalInMsec){  //???????????????????????????????????????
                            continue;
                        }
                        if(!lastExecutor.isShutdown()){ //???????????????task????????????
                            if(now - failedTime > taskMaxWaitInMsec){
                                markCommunicationFailed(taskId);
                                reportTaskGroupCommunication(lastTaskGroupContainerCommunication, taskCountInThisTaskGroup);
                                throw DataXException.asDataXException(CommonErrorCode.WAIT_TIME_EXCEED, "task failover????????????");
                            }else{
                                lastExecutor.shutdown(); //??????????????????
                                continue;
                            }
                        }else{
                            LOG.info("taskGroup[{}] taskId[{}] attemptCount[{}] has already shutdown",
                                    this.taskGroupId, taskId, lastExecutor.getAttemptCount());
                        }
                    }
                    Configuration taskConfigForRun = taskMaxRetryTimes > 1 ? taskConfig.clone() : taskConfig;
                	TaskExecutor taskExecutor = new TaskExecutor(taskConfigForRun, attemptCount);
                    taskStartTimeMap.put(taskId, System.currentTimeMillis());
                	taskExecutor.doStart();

                    iterator.remove();
                    runTasks.add(taskExecutor);

                    //???????????????task???runTasks??????????????????monitor????????????
                    taskMonitor.registerTask(taskId, this.containerCommunicator.getCommunication(taskId));

                    taskFailedExecutorMap.remove(taskId);
                    LOG.info("taskGroup[{}] taskId[{}] attemptCount[{}] is started",
                            this.taskGroupId, taskId, attemptCount);
                }

                //4.?????????????????????executor?????????, ???????????????success--->??????
                if (taskQueue.isEmpty() && isAllTaskDone(runTasks) && containerCommunicator.collectState() == State.SUCCEEDED) {
                	// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    lastTaskGroupContainerCommunication = reportTaskGroupCommunication(
                            lastTaskGroupContainerCommunication, taskCountInThisTaskGroup);

                    LOG.info("taskGroup[{}] completed it's tasks.", this.taskGroupId);
                    break;
                }

                // 5.?????????????????????????????????????????????interval?????????????????????????????????
                long now = System.currentTimeMillis();
                if (now - lastReportTimeStamp > reportIntervalInMillSec) {
                    lastTaskGroupContainerCommunication = reportTaskGroupCommunication(
                            lastTaskGroupContainerCommunication, taskCountInThisTaskGroup);

                    lastReportTimeStamp = now;

                    //taskMonitor?????????????????????task??????reportIntervalInMillSec????????????
                    for(TaskExecutor taskExecutor:runTasks){
                        taskMonitor.report(taskExecutor.getTaskId(),this.containerCommunicator.getCommunication(taskExecutor.getTaskId()));
                    }

                }

                Thread.sleep(sleepIntervalInMillSec);
            }

            //6.????????????????????????
            reportTaskGroupCommunication(lastTaskGroupContainerCommunication, taskCountInThisTaskGroup);


        } catch (Throwable e) {
            Communication nowTaskGroupContainerCommunication = this.containerCommunicator.collect();

            if (nowTaskGroupContainerCommunication.getThrowable() == null) {
                nowTaskGroupContainerCommunication.setThrowable(e);
            }
            nowTaskGroupContainerCommunication.setState(State.FAILED);
            this.containerCommunicator.report(nowTaskGroupContainerCommunication);

            throw DataXException.asDataXException(
                    FrameworkErrorCode.RUNTIME_ERROR, e);
        }finally {
            if(!PerfTrace.getInstance().isJob()){
                //????????????cpu??????????????????GC?????????
                VMInfo vmInfo = VMInfo.getVmInfo();
                if (vmInfo != null) {
                    vmInfo.getDelta(false);
                    LOG.info(vmInfo.totalString());
                }

                LOG.info(PerfTrace.getInstance().summarizeNoException());
            }
        }
    }
    
    private Map<Integer, Configuration> buildTaskConfigMap(List<Configuration> configurations){
    	Map<Integer, Configuration> map = new HashMap<Integer, Configuration>();
    	for(Configuration taskConfig : configurations){
        	int taskId = taskConfig.getInt(CoreConstant.TASK_ID);
        	map.put(taskId, taskConfig);
    	}
    	return map;
    }

    private List<Configuration> buildRemainTasks(List<Configuration> configurations){
    	List<Configuration> remainTasks = new LinkedList<Configuration>();
    	for(Configuration taskConfig : configurations){
    		remainTasks.add(taskConfig);
    	}
    	return remainTasks;
    }
    
    private TaskExecutor removeTask(List<TaskExecutor> taskList, int taskId){
    	Iterator<TaskExecutor> iterator = taskList.iterator();
    	while(iterator.hasNext()){
    		TaskExecutor taskExecutor = iterator.next();
    		if(taskExecutor.getTaskId() == taskId){
    			iterator.remove();
    			return taskExecutor;
    		}
    	}
    	return null;
    }
    
    private boolean isAllTaskDone(List<TaskExecutor> taskList){
    	for(TaskExecutor taskExecutor : taskList){
    		if(!taskExecutor.isTaskFinished()){
    			return false;
    		}
    	}
    	return true;
    }

    private Communication reportTaskGroupCommunication(Communication lastTaskGroupContainerCommunication, int taskCount){
        Communication nowTaskGroupContainerCommunication = this.containerCommunicator.collect();
        nowTaskGroupContainerCommunication.setTimestamp(System.currentTimeMillis());
        Communication reportCommunication = CommunicationTool.getReportCommunication(nowTaskGroupContainerCommunication,
                lastTaskGroupContainerCommunication, taskCount);
        this.containerCommunicator.report(reportCommunication);
        return reportCommunication;
    }

    private void markCommunicationFailed(Integer taskId){
        Communication communication = containerCommunicator.getCommunication(taskId);
        communication.setState(State.FAILED);
    }

    /**
     * TaskExecutor???????????????task????????????
     * ????????????1???1???reader???writer
     */
    class TaskExecutor {
        private Configuration taskConfig;

        private int taskId;

        private int attemptCount;

        private Channel channel;

        private Thread readerThread;

        private Thread writerThread;
        
        private ReaderRunner readerRunner;
        
        private WriterRunner writerRunner;

        /**
         * ?????????taskCommunication??????????????????
         * 1. channel
         * 2. readerRunner???writerRunner
         * 3. reader???writer???taskPluginCollector
         */
        private Communication taskCommunication;

        public TaskExecutor(Configuration taskConf, int attemptCount) {
            // ?????????taskExecutor?????????
            this.taskConfig = taskConf;
            Validate.isTrue(null != this.taskConfig.getConfiguration(CoreConstant.JOB_READER)
                            && null != this.taskConfig.getConfiguration(CoreConstant.JOB_WRITER),
                    "[reader|writer]???????????????????????????!");

            // ??????taskId
            this.taskId = this.taskConfig.getInt(CoreConstant.TASK_ID);
            this.attemptCount = attemptCount;

            /**
             * ???taskId?????????taskExecutor???Communication
             * ?????????readerRunner???writerRunner??????????????????channel????????????
             */
            this.taskCommunication = containerCommunicator
                    .getCommunication(taskId);
            Validate.notNull(this.taskCommunication,
                    String.format("taskId[%d]???Communication???????????????", taskId));
            this.channel = ClassUtil.instantiate(channelClazz,
                    Channel.class, configuration);
            this.channel.setCommunication(this.taskCommunication);

            /**
             * ??????transformer?????????
             */

            List<TransformerExecution> transformerInfoExecs = TransformerUtil.buildTransformerInfo(taskConfig);

            /**
             * ??????writerThread
             */
            writerRunner = (WriterRunner) generateRunner(PluginType.WRITER);
            this.writerThread = new Thread(writerRunner,
                    String.format("%d-%d-%d-writer",
                            jobId, taskGroupId, this.taskId));
            //????????????thread???contextClassLoader???????????????????????????????????????????????????
            this.writerThread.setContextClassLoader(LoadUtil.getJarLoader(
                    PluginType.WRITER, this.taskConfig.getString(
                            CoreConstant.JOB_WRITER_NAME)));

            /**
             * ??????readerThread
             */
            readerRunner = (ReaderRunner) generateRunner(PluginType.READER,transformerInfoExecs);
            this.readerThread = new Thread(readerRunner,
                    String.format("%d-%d-%d-reader",
                            jobId, taskGroupId, this.taskId));
            /**
             * ????????????thread???contextClassLoader???????????????????????????????????????????????????
             */
            this.readerThread.setContextClassLoader(LoadUtil.getJarLoader(
                    PluginType.READER, this.taskConfig.getString(
                            CoreConstant.JOB_READER_NAME)));
        }

        public void doStart() {
            this.writerThread.start();

            // reader???????????????writer???????????????
            if (!this.writerThread.isAlive() || this.taskCommunication.getState() == State.FAILED) {
                throw DataXException.asDataXException(
                        FrameworkErrorCode.RUNTIME_ERROR,
                        this.taskCommunication.getThrowable());
            }

            this.readerThread.start();

            // ??????reader??????????????????
            if (!this.readerThread.isAlive() && this.taskCommunication.getState() == State.FAILED) {
                // ?????????????????????Reader???????????????????????? ?????????????????? ????????????????????????
                throw DataXException.asDataXException(
                        FrameworkErrorCode.RUNTIME_ERROR,
                        this.taskCommunication.getThrowable());
            }

        }


        private AbstractRunner generateRunner(PluginType pluginType) {
            return generateRunner(pluginType, null);
        }

        private AbstractRunner generateRunner(PluginType pluginType, List<TransformerExecution> transformerInfoExecs) {
            AbstractRunner newRunner = null;
            TaskPluginCollector pluginCollector;

            switch (pluginType) {
                case READER:
                    newRunner = LoadUtil.loadPluginRunner(pluginType,
                            this.taskConfig.getString(CoreConstant.JOB_READER_NAME));
                    newRunner.setJobConf(this.taskConfig.getConfiguration(
                            CoreConstant.JOB_READER_PARAMETER));

                    pluginCollector = ClassUtil.instantiate(
                            taskCollectorClass, AbstractTaskPluginCollector.class,
                            configuration, this.taskCommunication,
                            PluginType.READER);

                    RecordSender recordSender;
                    if (transformerInfoExecs != null && transformerInfoExecs.size() > 0) {
                        recordSender = new BufferedRecordTransformerExchanger(taskGroupId, this.taskId, this.channel,this.taskCommunication ,pluginCollector, transformerInfoExecs);
                    } else {
                        recordSender = new BufferedRecordExchanger(this.channel, pluginCollector);
                    }

                    ((ReaderRunner) newRunner).setRecordSender(recordSender);

                    /**
                     * ??????taskPlugin???collector???????????????????????????job/task??????
                     */
                    newRunner.setTaskPluginCollector(pluginCollector);
                    break;
                case WRITER:
                    newRunner = LoadUtil.loadPluginRunner(pluginType,
                            this.taskConfig.getString(CoreConstant.JOB_WRITER_NAME));
                    newRunner.setJobConf(this.taskConfig
                            .getConfiguration(CoreConstant.JOB_WRITER_PARAMETER));

                    pluginCollector = ClassUtil.instantiate(
                            taskCollectorClass, AbstractTaskPluginCollector.class,
                            configuration, this.taskCommunication,
                            PluginType.WRITER);
                    ((WriterRunner) newRunner).setRecordReceiver(new BufferedRecordExchanger(
                            this.channel, pluginCollector));
                    /**
                     * ??????taskPlugin???collector???????????????????????????job/task??????
                     */
                    newRunner.setTaskPluginCollector(pluginCollector);
                    break;
                default:
                    throw DataXException.asDataXException(FrameworkErrorCode.ARGUMENT_ERROR, "Cant generateRunner for:" + pluginType);
            }

            newRunner.setTaskGroupId(taskGroupId);
            newRunner.setTaskId(this.taskId);
            newRunner.setRunnerCommunication(this.taskCommunication);

            return newRunner;
        }

        // ????????????????????????
        private boolean isTaskFinished() {
            // ??????reader ??? writer?????????????????????????????????????????????????????????
            if (readerThread.isAlive() || writerThread.isAlive()) {
                return false;
            }

            if(taskCommunication==null || !taskCommunication.isFinished()){
        		return false;
        	}

            return true;
        }
        
        private int getTaskId(){
        	return taskId;
        }

        private long getTimeStamp(){
            return taskCommunication.getTimestamp();
        }

        private int getAttemptCount(){
            return attemptCount;
        }
        
        private boolean supportFailOver(){
        	return writerRunner.supportFailOver();
        }

        private void shutdown(){
            writerRunner.shutdown();
            readerRunner.shutdown();
            if(writerThread.isAlive()){
                writerThread.interrupt();
            }
            if(readerThread.isAlive()){
                readerThread.interrupt();
            }
        }

        private boolean isShutdown(){
            return !readerThread.isAlive() && !writerThread.isAlive();
        }
    }
}
