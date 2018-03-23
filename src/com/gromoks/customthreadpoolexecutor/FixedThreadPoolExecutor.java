package com.gromoks.customthreadpoolexecutor;

import java.util.*;
import java.util.concurrent.*;

public class FixedThreadPoolExecutor implements ExecutorService {
    private final Queue<Runnable> runnableTaskQueue = new LinkedList<>();
    private final List<Thread> threadPool;
    private final int capacity;
    private volatile boolean isActive;

    public FixedThreadPoolExecutor(int capacity) {
        isActive = true;
        this.capacity = capacity;
        threadPool = new ArrayList<>(capacity);
    }

    @Override
    public void shutdown() {
        isActive = false;
        synchronized (runnableTaskQueue) {
            runnableTaskQueue.notifyAll();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> runnableList = new ArrayList<>();

        shutdown();
        threadPool.forEach(Thread::interrupt);

        synchronized (runnableTaskQueue) {
            runnableList.addAll(runnableTaskQueue);
            runnableTaskQueue.clear();
        }
        return runnableList;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return runnableTaskQueue.isEmpty() && !isActive;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {
        if (isActive) {
            if (threadPool.size() < capacity) {
                threadInit();
            }
            synchronized (runnableTaskQueue) {
                runnableTaskQueue.offer(command);
                runnableTaskQueue.notify();
            }
        }
    }

    public void printThreadState() {
        for (Thread thread : threadPool) {
            System.out.println("Status of " + thread.getName() + " - " + thread.getState());
        }
    }

    private void threadInit() {
        Runnable taskRunner = this::taskRunner;
        Thread thread = new Thread(taskRunner);
        threadPool.add(thread);
        thread.start();
    }

    private void taskRunner() {
        while (!isTerminated()) {
            Runnable task;

            synchronized (runnableTaskQueue) {
                while (runnableTaskQueue.isEmpty()) {
                    try {
                        runnableTaskQueue.wait();
                        if (!isActive) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        System.out.println("An error occurred for " + Thread.currentThread().getName() + " while queue is waiting: " + e.getMessage());
                        break;
                    }
                }

                if (!Thread.currentThread().isInterrupted()) {
                    task = runnableTaskQueue.poll();
                } else {
                    break;
                }
            }

            if (task != null) {
                String name = Thread.currentThread().getName();
                System.out.println("Task Started by Thread :" + name);
                task.run();
                System.out.println("Task Finished by Thread :" + name);
            }
        }
    }
}

