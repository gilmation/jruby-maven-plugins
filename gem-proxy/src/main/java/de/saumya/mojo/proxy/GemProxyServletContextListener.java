package de.saumya.mojo.proxy;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import de.saumya.mojo.ruby.ScriptingService;

public class GemProxyServletContextListener implements ServletContextListener {

    static class Updater extends Thread {

        private volatile boolean        isRunning = true;

        private final ControllerService controller;

        private final ServletContext    context;

        // private static long lastUpdate = 0;

        Updater(final ControllerService controller, final ServletContext context) {
            this.controller = controller;
            this.context = context;
        }

        private void log(final String msg) {
            this.context.log(msg + " " + this);
        }

        private void log(final String msg, final Exception e) {
            this.context.log(msg + " " + this, e);
        }

        @Override
        public void run() {
            // TODO better logging via slf4j
            log("started update job");
            while (this.isRunning) {
                try {
                    // 12 hours
                    Thread.sleep(12 * 60 * 60 * 1000);
                    this.controller.update();
                    log("updated metadata");
                }
                catch (final InterruptedException e) {
                    log("interrupted", e);
                }
                catch (final RuntimeException e) {
                    log("maybe bug ?!", e);
                }
            }
            log("stopped update job");
        }
    }

    private Updater updater;

    private Thread  thread;

    public void contextDestroyed(final ServletContextEvent sce) {
        sce.getServletContext().log("stopping background job . . .");
        this.updater.isRunning = false;
        this.thread.interrupt();
        try {
            this.thread.join();
        }
        catch (final InterruptedException e) {
        }
    }

    public void contextInitialized(final ServletContextEvent sce) {
        final ScriptingService scripting = new ScriptingService();
        ControllerService controller;
        sce.getServletContext().log("registering "
                + ControllerService.class.getName() + " . . .");

        try {
            controller = new ControllerService(scripting);
        }
        catch (final IOException e) {
            throw new RuntimeException("error initializing controller", e);
        }
        sce.getServletContext().log("registered "
                + ControllerService.class.getName());
        sce.getServletContext().setAttribute(ControllerService.class.getName(),
                                             controller);

        this.updater = new Updater(controller, sce.getServletContext());
        this.thread = this.updater;
        this.thread.start();
    }

}
