/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.worker;

import ca.phon.worker.PhonTask.TaskStatus;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A worker thread for the application.
 * Tasks are place on the worker thread using
 * the invokeLater(Runnable) method.
 * 
 * Tasks are run FIFO.
 *
 */
public class PhonWorker extends Thread {
	
	/** The worker thread name */
	private static final String STATIC_THREAD_NAME = "PhonWorker-";
	private static volatile int STATIC_THREAD_NUM = 0;
	
	/** The queue */
	private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
	
	/** The static instance */
	private volatile static PhonWorker _instance;
	
	/** The shutdown thread */
	private static PhonWorker _shutdownThread;
	
	/** Is this thread running? */
	private volatile boolean shutdown = false;
	
	private volatile boolean haltOnError = false;
	
	private volatile boolean finishWhenQueueEmpty = false;
	
	/** Task which is run even after shutdown() has been called */
	private Runnable finalTask;
	
	/** Get the static instance */
	public static PhonWorker getInstance() {
		if(_instance == null || !_instance.isAlive())
			_instance = new PhonWorker();
		return _instance;
	}
	
	/**
	 * Creates a new PhonWorker thread and returns
	 * it.  The new thread is not maintained by this
	 * class and the reference to it should be kept and
	 * shutdown by the requsting class.
	 * 
	 * @return a new PhonWorker thread
	 */
	public static PhonWorker createWorker() {
		return new PhonWorker();
	}
	
	/**
	 * Create a new worker using a shared queue.
	 * 
	 * @return a new PhonWorker thread
	 */
	public static PhonWorker createWorker(ConcurrentLinkedQueue<Runnable> queue) {
		PhonWorker retVal = new PhonWorker();
		retVal.queue = queue;
		return retVal;
	}
	
	private static String getNextThreadName() {
		return STATIC_THREAD_NAME + (STATIC_THREAD_NUM++);
	}
	
	/** Constructor */
	protected PhonWorker() {
		super(getNextThreadName());
		
		super.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				final Logger LOGGER = Logger.getLogger(PhonWorker.class.getName());
				LOGGER.severe("Error in thread '" + getName() + "'");
				LOGGER.log(Level.SEVERE, e.toString(), e);
				if(finalTask != null) {
					try {
						finalTask.run();
					} catch (Exception ex) {
						LOGGER.warning(ex.toString());
					}
				}
			}
			
		});
	}

	public static PhonTask invokeOnNewWorker(Runnable toRun) {
		return invokeOnNewWorker(toRun, () -> {}, (err) -> {});
	}

	public static PhonTask invokeOnNewWorker(Runnable toRun, Runnable onFinish) {
		return invokeOnNewWorker(toRun, onFinish, (err) -> {});
	}

	/**
	 * Invoke provided runnable on a new thread and return the generated PhonTask
	 *
	 * @param toRun
	 * @param onFinish
	 * @Param onError
	 */
	public static PhonTask invokeOnNewWorker(Runnable toRun, Runnable onFinish, PhonTaskErrorHandler onError) {
		PhonWorker worker = PhonWorker.createWorker();

		final PhonTask retVal = new PhonTask() {
			@Override
			public void performTask() {
				setStatus(TaskStatus.RUNNING);
				try {
					toRun.run();
					setStatus(TaskStatus.FINISHED);
				} catch (Exception e) {
					super.err = e;
					setStatus(TaskStatus.ERROR);

					onError.handleError(e);
				}
			}
		};
		worker.invokeLater(retVal);

		worker.setFinalTask(onFinish);
		worker.setFinishWhenQueueEmpty(true);
		worker.start();

		return retVal;
	}
	
	/**
	 * Add a task to the queue.
	 *
	 * @param task
	 */
	public void invokeLater(Runnable task) {
		queue.add(task);
	}
	
	@Override
	public void run() {
		shutdown = false;
		final Logger LOGGER = Logger.getLogger(PhonWorker.class.getName());
		LOGGER.log(Level.FINE, "Starting worker thread: " + getName());
		while(!shutdown) {
			// poll instead of peek then poll as other threads may then acquire the task first
			Runnable nextTask = queue.poll();
			if(nextTask != null) {
				// run the next task in the queue
				try {
					nextTask.run();

					if(nextTask instanceof PhonTask) {
						PhonTask pt = (PhonTask)nextTask;
						if(pt.getStatus() == TaskStatus.ERROR) {
							if(pt.getException() != null)
								throw pt.getException();
							else
								throw new RuntimeException("Error");
						}
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
					if(!haltOnError)
						continue;
					else
						shutdown = true;
				}
			} else {
				if(this != _shutdownThread && !finishWhenQueueEmpty) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
						shutdown = true;
						continue;
					}
				} else {
					shutdown = true;
				}
			}
		}
		
		if(finalTask != null) {
			try {
				finalTask.run();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		
		LOGGER.log(Level.FINE, "Worker thread finished: " + getName());
	}
	
	public static PhonWorker getShutdownThread() {
		if(_shutdownThread == null) {
			_shutdownThread = PhonWorker.createWorker();
			_shutdownThread.setName("Phon Shutdown Hook");
		}
		return _shutdownThread;
	}
	
	/**
	 * Shutdown the worker thread.  The currently executing task
	 * must complete first.
	 */
	public void shutdown() {
		shutdown = true;
		Logger.getLogger(PhonWorker.class.getName()).log(Level.FINE, "Shutting down worker thread: " + getName());
	}
	
	/**
	 * Tells if the currently running thread
	 * is the worker thread.
	 * 
	 * @return true if we are running in the worker thread,
	 * false otherwise
	 */
	public static boolean isStaticWorkerThread() {
		if(_instance == null) return false;
		Thread currentThread = Thread.currentThread();
		if(currentThread.getName().equals(_instance.getName()))
			return true;
		else
			return false;
	}
	
	public boolean hasTasks() {
		return queue.isEmpty();
	}

	public boolean isHaltOnError() {
		return haltOnError;
	}

	public void setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	public Runnable getFinalTask() {
		return finalTask;
	}

	public void setFinalTask(Runnable finalTask) {
		this.finalTask = finalTask;
	}
	
	public Collection<Runnable> getTasks() {
//		Runnable[] tasks = queue.toArray(new Runnable[0]);
		ArrayList<Runnable> retVal = 
			new ArrayList<Runnable>();
		retVal.addAll(queue);
		
		return retVal;
	}

	public boolean isFinishWhenQueueEmpty() {
		return finishWhenQueueEmpty;
	}

	public void setFinishWhenQueueEmpty(boolean finishWhenQueueEmpty) {
		this.finishWhenQueueEmpty = finishWhenQueueEmpty;
	}
}
