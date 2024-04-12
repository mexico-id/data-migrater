package io.mosip.packet.core.constant;

import io.mosip.packet.core.service.thread.CustomizedThreadPoolExecutor;
import io.mosip.packet.core.util.FixedListQueue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class GlobalConfig {

    public static Boolean IS_ONLY_FOR_QUALITY_CHECK = false;

    public static Boolean WRITE_BIOSDK_RESPONSE = false;

    public static String SESSION_KEY;

    public static Boolean IS_TRACKER_REQUIRED = true;

    public static Boolean IS_NETWORK_AVAILABLE = true;

    public static Boolean IS_RUNNING_AS_BATCH = false;

    public static FixedListQueue<Long> TIMECONSUPTIONQUEUE;

    public static Long TOTAL_RECORDS_FOR_PROCESS = 0l;

    public static Long TOTAL_FAILED_RECORDS = 0l;

 //   public static Long ALREADY_PROCESSED_RECORDS = 0L;

    public static Boolean IS_TPM_AVAILABLE;

    public static Boolean IS_DATABASE_READ_OPERATION = false;

    public static Boolean IS_PACKET_UPLOAD_OPERATION = false;

    public static Long NO_OF_PACKETS_UPLOADED = 0L;

    public static List<CustomizedThreadPoolExecutor> THREAD_POOL_EXECUTOR_LIST = new ArrayList<>();

    public static String getActivityName() {
        return (IS_ONLY_FOR_QUALITY_CHECK ? "QUALITY ANALYSIS" : "PACKET CREATOR");
    }

    public static boolean isThreadPoolCompleted() throws InterruptedException {
        boolean isCompleted = true;

        for(CustomizedThreadPoolExecutor executor : THREAD_POOL_EXECUTOR_LIST)
            if(!executor.isBatchAcceptRequest())
                isCompleted = false;

        if(!isTaskCompleted())
            isCompleted = false;

        return isCompleted;
    }

    public static Long getPendingCountForProcess() {
        Long pendingCount = 0L;
        for(CustomizedThreadPoolExecutor executor : THREAD_POOL_EXECUTOR_LIST) {
            if(executor.getNAME().equals("QUALITY ANALYSIS") || executor.getNAME().equals("PACKET CREATOR")) {
                pendingCount += executor.getCurrentPendingCount();
            }
        }
        return pendingCount;
    }

    public static boolean isTaskCompleted() throws InterruptedException {
        boolean isCompleted = true;

        if(!IS_DATABASE_READ_OPERATION && !IS_PACKET_UPLOAD_OPERATION)
            for(CustomizedThreadPoolExecutor executor : THREAD_POOL_EXECUTOR_LIST) {
                for(ThreadPoolExecutor entry : executor.getPoolMap()) {
                    if(entry.getActiveCount() > 0) {
                        isCompleted = false;
                        break;
                    } else {
                        if(executor.getInputProcessCompleted() && (entry.getTaskCount() - entry.getCompletedTaskCount() <= 0) && executor.isBatchAcceptRequest() && executor.getCurrentPendingCount() <= 0) {
                            if(executor.getNAME().equals("QUALITY ANALYSIS")) {
                                if(TOTAL_RECORDS_FOR_PROCESS - TOTAL_FAILED_RECORDS - executor.getCurrentCompletedTask() <= 0 || executor.getCountOfZeroActiveCount() > 10) {
                                    if(executor.getWatch() != null)
                                        executor.getWatch().cancel();
                                    if(executor.getEstimateTimer() != null)
                                        executor.getEstimateTimer().cancel();
                                    if(executor.getSlotAllocationTimer() != null)
                                        executor.getSlotAllocationTimer().cancel();
                                } else {
                                    isCompleted = false;
                                }
                            } else {
                                if(executor.getWatch() != null)
                                    executor.getWatch().cancel();
                                if(executor.getEstimateTimer() != null)
                                    executor.getEstimateTimer().cancel();
                                if(executor.getSlotAllocationTimer() != null)
                                    executor.getSlotAllocationTimer().cancel();
                            }
                        } else {
                            isCompleted = false;
                        }
                    }
                }
            }

        if(IS_DATABASE_READ_OPERATION || IS_PACKET_UPLOAD_OPERATION)
            isCompleted = false;

        return isCompleted;
    }
}
