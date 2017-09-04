package io.playpen.visual.util;

import io.playpen.visual.controller.LogTabController;
import javafx.application.Platform;
import lombok.Setter;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(name = "WorkspaceAppender", category = "Core", elementType = "appender", printObject = true)
public class WorkspaceLogAppender extends AbstractAppender {
    private static WorkspaceLogAppender instance;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    @Setter
    private LogTabController logTabController;
    private final Queue<String> logQueue = new LinkedList<>();

    private WorkspaceLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);

        instance = this;
    }

    public static WorkspaceLogAppender get() {
        return instance;
    }

    @PluginFactory
    public static WorkspaceLogAppender createAppender(@PluginAttribute("name") String name,
                                                      @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                      @PluginElement("Filter") final Filter filter) {

        if (name == null) {
            LOGGER.error("No name provided for WorkspaceLogAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new WorkspaceLogAppender(name, filter, layout, true);
    }

    public void pumpQueue() {
        synchronized (logQueue) {
            while (!logQueue.isEmpty()) {
                logTabController.log(logQueue.remove());
            }
        }
    }

    @Override
    public void append(LogEvent event) {
        try {
            String message = new String(getLayout().toByteArray(event));
            synchronized (logQueue) {
                logQueue.add(message);
            }

            Platform.runLater(this::pumpQueue);
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        }
    }
}
