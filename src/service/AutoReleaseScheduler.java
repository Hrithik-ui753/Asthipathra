package service;

import javax.swing.Timer;

public class AutoReleaseScheduler {
    private final ReleaseService releaseService = new ReleaseService();
    private Timer timer;

    public void start() {
        if (timer != null && timer.isRunning()) {
            return;
        }
        // Run once immediately, then every 2 minutes.
        releaseService.runInactivityAutoReleaseSweep();
        timer = new Timer(120_000, e -> releaseService.runInactivityAutoReleaseSweep());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
