/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.nonpersistenttimer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
public class TimerSessionBean {
    @Resource
    TimerService timerService;

    private boolean runMethods = true;
    private final List<String> messages = Collections.synchronizedList(new ArrayList<String>());

    public void createTimer(long intervalDuration, String info, boolean repeat) {

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo(info);
        if (repeat) {
            ScheduleExpression schedule = new ScheduleExpression();
            schedule.hour("*");
            schedule.minute("*");
            schedule.second("*/" + intervalDuration);
            timerService.createCalendarTimer(schedule, tc);
        } else {
            timerService.createSingleActionTimer(intervalDuration * 1000, tc);
        }
    }

    public boolean uniqueInfo(String info) {
        Collection<Timer> timers = timerService.getTimers();
        for (Timer t : timers) {
            if (t.getInfo() != null && t.getInfo().toString().equals(info)) {
                return false;
            }
        }
        return true;
    }

    private String getCurrentTime() {
        Date now = new Date();
        SimpleDateFormat time = new SimpleDateFormat("kk:mm:ss");
        return time.format(now);
    }

    public Collection<Timer> getTimers() {
        return timerService.getTimers();
    }

    @Timeout
    public void programmaticTimeout(Timer timer) {
        messages.add(timer.getInfo().toString() + " : " + getCurrentTime());
        if (messages.size() > 100) {
            messages.remove(0);
        }
    }

    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    public void automaticTimeout() {
        messages.add("Automatic Timer : " + getCurrentTime());
        if (messages.size() > 100) {
            messages.remove(0);
        }

    }

    public List<String> getMessagesSent() {
        return messages;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void printAsyncMessage() {
        int counter = 0;
        long start = System.nanoTime();
        while (runMethods) {
            try {
                if (counter == 0) {
                    messages.add("Async Method 1 : Started");
                    if (messages.size() > 100) {
                        messages.remove(0);
                    }
                } else if (counter % 10 == 0) {
                    long current = System.nanoTime();
                    int seconds = (int) ((current - start) / 1000000000.0 + .5);
                    messages.add("Async Method 1 : Has been running for " + seconds + " seconds");
                    if (messages.size() > 100) {
                        messages.remove(0);
                    }
                }
                counter++;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (counter > 0) {
            long current = System.nanoTime();
            int seconds = (int) ((current - start) / 1000000000.0 + .5);
            messages.add("Async Method 1 : Completed after " + seconds + " seconds");
            if (messages.size() > 100) {
                messages.remove(0);
            }
        }
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void printAsyncMessage2() {
        int counter = 0;
        long start = System.nanoTime();
        while (runMethods) {
            try {
                if (counter == 0) {
                    messages.add("Async Method 2 : Started");
                    if (messages.size() > 100) {
                        messages.remove(0);
                    }
                } else if (counter % 15 == 0) {
                    long current = System.nanoTime();
                    int seconds = (int) ((current - start) / 1000000000.0 + .5);
                    messages.add("Async Method 2 : Has been running for " + seconds + " seconds");
                    if (messages.size() > 100) {
                        messages.remove(0);
                    }
                }
                counter++;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (counter > 0) {
            long current = System.nanoTime();
            int seconds = (int) ((current - start) / 1000000000.0 + .5);
            messages.add("Async Method 2 : Completed after " + seconds + " seconds");
            if (messages.size() > 100) {
                messages.remove(0);
            }
        }
    }

    public void startAsyncMethods() {
        runMethods = true;
    }

    public void stopAsyncMethods() {
        runMethods = false;
    }

    public Collection<Timer> getTs() {
        return timerService.getTimers();
    }

    public Timer getTimer(String timerInfo) {
        Collection<Timer> ts = timerService.getTimers();
        for (Timer t : ts) {
            if (t.toString().substring(0, 20).equals(timerInfo.substring(0, 20))) {
                return t;
            }
        }
        return null;
    }

    public void cancelTimer(String ti) {
        Collection<Timer> ts = timerService.getTimers();
        for (Timer t : ts) {
            if (t.getInfo() != null && t.getInfo().toString().equals(ti)) {
                t.cancel();
            } else if (t.getInfo() == null && ti.equals("Automatic Timer")) {
                t.cancel();
            }
        }
    }
}
