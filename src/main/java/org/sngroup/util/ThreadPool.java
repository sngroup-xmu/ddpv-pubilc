package org.sngroup.util;

import java.util.concurrent.*;

public class ThreadPool {
    protected ThreadPoolExecutor threadPool;

    final BlockingQueue<Future<?>> futures;

    boolean _isShutdown = false;

    public ThreadPool(){
        futures = new LinkedBlockingQueue<>();
    }

    public static ThreadPool FixedThreadPool(int size){
        ThreadPool t = new ThreadPool();
        t.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(size);
        return t;
    }
    public void execute(Runnable command) {
        Future<?> future = threadPool.submit(command);
        futures.add(future);
    }

    public void awaitAllTaskFinished(){
        awaitAllTaskFinished(100);
    }

    public synchronized void awaitAllTaskFinished(int timeout){
        while (!futures.isEmpty()) {
            Future<?> future;
            try {
                future = futures.poll(timeout, TimeUnit.MICROSECONDS);
            } catch (InterruptedException e) {
                break;
            }
            if (future == null) continue;
            try {
                future.get();
            } catch (InterruptedException e) {
                break;
            } catch (ExecutionException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void shutdownNow(){
        threadPool.shutdownNow();
        _isShutdown = true;
    }

    public boolean isShutdown(){
        return _isShutdown;
    }
}
