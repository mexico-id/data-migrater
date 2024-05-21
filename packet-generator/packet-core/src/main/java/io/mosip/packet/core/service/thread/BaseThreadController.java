package io.mosip.packet.core.service.thread;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.logger.DataProcessLogger;
import lombok.Setter;

import static io.mosip.packet.core.constant.RegistrationConstants.APPLICATION_ID;
import static io.mosip.packet.core.constant.RegistrationConstants.APPLICATION_NAME;

@Setter
public abstract class BaseThreadController implements Runnable {
    protected static final Logger LOGGER = DataProcessLogger.getLogger(BaseThreadController.class);
    protected String poolName;
    protected ResultSetter setter;
    protected CountIncrementer failedRecordCount;
    protected SuuccessResponse response;

    protected interface SuuccessResponse {
        public void onSuccess();
        public void onFailure();
    }

    @Override
    public void run() {
        try {
            execute();
            response.onSuccess();
        } catch (Exception e) {
            failedRecordCount.increment();
            response.onFailure();
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Processing Data  : " + ExceptionUtils.getStackTrace(e));
        }
    }

    public abstract void execute() throws Exception;
}
