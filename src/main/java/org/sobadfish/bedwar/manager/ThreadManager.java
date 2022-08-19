package org.sobadfish.bedwar.manager;

import org.sobadfish.bedwar.BedWarMain;
import org.sobadfish.bedwar.room.GameRoom;
import org.sobadfish.bedwar.thread.PluginMasterRunnable;
import org.sobadfish.bedwar.thread.RoomLoadRunnable;
import org.sobadfish.bedwar.thread.TopRunnable;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author SoBadFish
 * 2022/1/2
 */
public class ThreadManager {


    public static final Timer timer = new Timer();

    public static final List<AbstractBedWarRunnable> RUNNABLES = new CopyOnWriteArrayList<>();

    // 线程池核心线程数
    private final static Integer CORE_POOLSIZE = 1;


    private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(CORE_POOLSIZE,new ThreadPoolExecutor.AbortPolicy());


    public static void cancel(AbstractBedWarRunnable r) {
        RUNNABLES.remove(r);
        scheduled.remove(r);
    }

    private static void schedule(AbstractBedWarRunnable r) {
        RUNNABLES.add(r);
        scheduled.scheduleAtFixedRate(r,0,1,TimeUnit.SECONDS);
    }


    /**
     * 获取当前线程池线程数量
     */
    public static int getScheduledSize() {
        return scheduled.getPoolSize();
    }

    /**
     * 获取当前活动的线程数量
     */
    public static int getScheduledActiveCount() {
        return scheduled.getActiveCount();
    }

    public static void addDelayThread(){
        timer.cancel();
    }


    public static void addScheduled(AbstractBedWarRunnable runnable){
        if(RUNNABLES.contains(runnable)){
            AbstractBedWarRunnable runnable1 = RUNNABLES.get(RUNNABLES.indexOf(runnable));
            runnable1.isClose = true;
        }
        schedule(runnable);
    }

    public static String info() {
        StringBuilder builder = new StringBuilder();
        Map<String, List<AbstractBedWarRunnable>> map = getRunnables();
        for(Map.Entry<String, List<AbstractBedWarRunnable>> me : map.entrySet()){
            builder.append("&r").append(me.getKey()).append("\n").append(listToString(me.getValue()));
        }
        String s = builder.toString();
        if("".equalsIgnoreCase(s)){
            return "null";
        }
        return s;
    }

    private static String listToString(List<AbstractBedWarRunnable> runnables){
        StringBuilder s = new StringBuilder();
        for(AbstractBedWarRunnable runnable: runnables){
            s.append("  &r- ").append(runnable.getThreadName()).append("\n");
        }
        return s.toString();
    }

    private static Map<String,List<AbstractBedWarRunnable>> getRunnables(){
        LinkedHashMap<String, List<AbstractBedWarRunnable>> threadList = new LinkedHashMap<>();

        for(AbstractBedWarRunnable workerValue: RUNNABLES) {
            GameRoom room = workerValue.getRoom();
            if (room != null) {
                if (!threadList.containsKey(room.getRoomConfig().name)) {
                    threadList.put(room.getRoomConfig().name, new ArrayList<>());
                }
                List<AbstractBedWarRunnable> runnables = threadList.get(room.getRoomConfig().name);
                runnables.add(workerValue);
                threadList.put(room.getRoomConfig().name, runnables);
            } else {
                String name = "Unknown";
                if (!threadList.containsKey(name)) {
                    threadList.put(name, new ArrayList<>());
                }
                List<AbstractBedWarRunnable> runnables = threadList.get(name);
                runnables.add(workerValue);
                threadList.put(name, runnables);
            }
        }
        return threadList;
    }

    public static void init() {
        ThreadManager.schedule(new RunnableCheck());
        ThreadManager.schedule(new PluginMasterRunnable());
        ThreadManager.schedule(new RoomLoadRunnable());
        ThreadManager.schedule(new TopRunnable());

    }


    public abstract static class AbstractBedWarRunnable implements Runnable{

        public boolean isClose;

        abstract public GameRoom getRoom();

        abstract public String getThreadName();

        public boolean isClose() {
            return isClose;
        }
    }

    public static class RunnableCheck extends AbstractBedWarRunnable{
        @Override
        public GameRoom getRoom() {
            return null;
        }

        @Override
        public String getThreadName() {
            String color = "&a";
            if(isClose){
                color = "&7";
            }
            return color+"线程检测";
        }

        @Override
        public void run() {
            if(isClose){
                return;
            }
            if(BedWarMain.getBedWarMain().isDisabled()){
                isClose = true;
                return;
            }
            for (AbstractBedWarRunnable runnable : RUNNABLES) {
                if (runnable.isClose) {
                    cancel(runnable);
                }
            }
        }

    }



}
