/*
 * Copyright 2012-2015 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.screenshot.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandGenerator extends BaseScreenShotGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CommandGenerator.class);

    public List<String> commandList;

    public long commandTimeout = 10 * 1000;// 10sec

    public File baseDir;

    private volatile Timer destoryTimer;

    @PostConstruct
    public void init() {
        if (baseDir == null) {
            baseDir = new File(application.getRealPath("/"));
        }
        destoryTimer = new Timer("CommandGeneratorDestoryTimer-" + System.currentTimeMillis(), true);
    }

    @PreDestroy
    public void destroy() {
        destoryTimer.cancel();
        destoryTimer = null;
    }

    @Override
    public void generate(final String url, final File outputFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("Generate ScreenShot: " + url);
        }

        if (outputFile.exists()) {
            if (logger.isDebugEnabled()) {
                logger.debug("The screenshot file exists: " + outputFile.getAbsolutePath());
            }
            return;
        }

        final File parentFile = outputFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        if (!parentFile.isDirectory()) {
            logger.warn("Not found: " + parentFile.getAbsolutePath());
            return;
        }

        final String outputPath = outputFile.getAbsolutePath();
        final List<String> cmdList = new ArrayList<String>();
        for (final String value : commandList) {
            cmdList.add(value.replace("${url}", url).replace("${outputFile}", outputPath));
        }

        ProcessBuilder pb = null;
        Process p = null;

        if (logger.isDebugEnabled()) {
            logger.debug("ScreenShot Command: " + cmdList);
        }

        TimerTask task = null;
        try {
            pb = new ProcessBuilder(cmdList);
            pb.directory(baseDir);
            pb.redirectErrorStream(true);

            p = pb.start();

            task = new ProcessDestroyer(p, cmdList);
            try {
                destoryTimer.schedule(task, commandTimeout);

                String line;
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));
                    while ((line = br.readLine()) != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(line);
                        }
                    }
                } finally {
                    IOUtils.closeQuietly(br);
                }

                p.waitFor();
            } catch (final Exception e) {
                p.destroy();
            }
        } catch (final Exception e) {
            logger.warn("Failed to generate a screenshot of " + url, e);
        }
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (outputFile.isFile() && outputFile.length() == 0) {
            logger.warn("ScreenShot File is empty. URL is " + url);
            if (outputFile.delete()) {
                logger.info("Deleted: " + outputFile.getAbsolutePath());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("ScreenShot File: " + outputPath);
        }
    }

    protected static class ProcessDestroyer extends TimerTask {

        private final Process p;

        private final List<String> commandList;

        protected ProcessDestroyer(final Process p, final List<String> commandList) {
            this.p = p;
            this.commandList = commandList;
        }

        @Override
        public void run() {
            logger.warn("CommandGenerator is timed out: " + commandList);
            try {
                p.destroy();
            } catch (final Exception e) {
                logger.warn("Failed to stop destroyer.", e);
            }
        }
    }

}
