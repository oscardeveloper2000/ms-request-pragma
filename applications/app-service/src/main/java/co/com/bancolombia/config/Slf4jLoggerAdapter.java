package co.com.bancolombia.config;


import co.com.bancolombia.model.common.gateways.LoggerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


public class Slf4jLoggerAdapter implements LoggerPort {

    private static final Logger logger = LoggerFactory.getLogger("application");

    @Override public void info(String msg, Object... args) { if (logger.isInfoEnabled()) logger.info(msg, args); }
    @Override public void warn(String msg, Object... args) { if (logger.isWarnEnabled()) logger.warn(msg, args); }
    @Override public void debug(String msg, Object... args) { if (logger.isDebugEnabled()) logger.debug(msg, args); }
    @Override public void error(String msg, Object... args) { if (logger.isErrorEnabled()) logger.error(msg, args); }
    @Override public void error(String msg, Throwable t) { logger.error(msg, t); }
}
