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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class PhonWorkerGroup {

	/** The queue of tasks to complete */
	private ConcurrentLinkedQueue<Runnable> tasks;

	private Runnable finalTask = null;

	/** The array of running tasks */
	private PhonWorker[] runningTasks;
	
	/** Shutdown hook */
	private boolean shutdown = false;

	private int totalTasks = 0;

	private int completedTasks = 0;
	
	/**
	 * Create a new task manager with the specified
	 * number of concurrent tasks.
	 * 
	 * @param taskWindow
	 */
	public PhonWorkerGroup(int taskWindow) {
		super();

		this.tasks = new ConcurrentLinkedQueue<Runnable>();
		this.runningTasks = new PhonWorker[taskWindow];
		for (int i = 0; i < taskWindow; i++) {
			this.runningTasks[i] = PhonWorker.createWorker(this.tasks);
			this.runningTasks[i].addPropertyChangeListener("currentTask", (e) -> {
				if(e.getNewValue() == null) {
					completedTasks++;
					if(totalTasks > 0 && completedTasks == totalTasks) {
						if(finalTask != null) {
							finalTask.run();
						}
						shutdown();
					}
				}
			});
		}
	}

	/**
	 * Add a new task to the queue.
	 */
	public void queueTask(Runnable t) {
		tasks.add(t);
	}
	
	public void begin() {
		shutdown = false;
		
		// startup all worker threads
		for(int i = 0; i < runningTasks.length; i++)
			runningTasks[i].start();
	}
	
	public void shutdown() {
		shutdown = true;
		
		// tell the worker threads to shutdown
		for(int i = 0; i < runningTasks.length; i++)
			runningTasks[i].shutdown();
	}

	public boolean isShutdown() {
		return shutdown;
	}
	
	public Collection<Runnable> getTaskList() {
		return tasks;
	}
	
	public PhonWorker[] getThreads() {
		return runningTasks;
	}

	public void setTotalTasks(int totalTasks) {
		this.totalTasks = totalTasks;
	}

	public void setFinalTask(Runnable finalTask) {
		this.finalTask = finalTask;
	}

}
