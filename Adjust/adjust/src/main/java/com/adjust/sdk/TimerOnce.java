package com.adjust.sdk;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pfms on 08/05/15.
 */
public class TimerOnce {
    private ScheduledExecutorService scheduler;
    private ScheduledFuture waitingTask;
    private String name;
    private Runnable command;
    private ILogger logger;

    public TimerOnce(ScheduledExecutorService scheduler, Runnable command, String name) {
        this.name = name;
        this.scheduler = scheduler;
        this.command = command;
        this.logger = AdjustFactory.getLogger();
    }

    public void startIn(long fireIn) {
        // cancel previous
        cancel(false);

        logger.verbose("%s starting. Launching in %d seconds", name, TimeUnit.MILLISECONDS.toSeconds(fireIn));

        waitingTask = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                logger.verbose("%s fired", name);
                command.run();
                waitingTask = null;
            }
        }, fireIn, TimeUnit.MILLISECONDS);
    }

    public long getFireIn() {
        if (waitingTask == null) {
            return 0;
        }
        return waitingTask.getDelay(TimeUnit.MILLISECONDS);
    }

    private void cancel(boolean log) {
        if (waitingTask != null) {
            waitingTask.cancel(false);
        }
        waitingTask = null;

        if (log) {
            logger.verbose("%s canceled", name);
        }
    }

    public void cancel() {
        cancel(true);
    }
}
