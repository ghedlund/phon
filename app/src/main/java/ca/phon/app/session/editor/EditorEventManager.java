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
package ca.phon.app.session.editor;

import ca.phon.app.log.LogUtil;
import ca.phon.util.PrefHelper;
import ca.phon.worker.*;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;

/**
 * <p>Handles passing of internal EditorEvents.  Events can be dispatched on the AWT event
 * thread or on a background thread.</p>
 * 
 * <p>Events are placed into a queue until they can be processed.  Events are processed
 * using a background thread.</p>
 * 
 */
public class EditorEventManager {
	
	/**
	 * Event queue.  Events are placed in the queue until they
	 * can be processed.
	 */
	private final BlockingQueue<EditorEvent<?>> eventQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Event dispatch thread for the editor
	 */
	private final PhonWorker dispatchThread = PhonWorker.createWorker();
	
	/**
	 * action map
	 */
	private final Map<EditorEventType<?>, List<EditorEventHandler<?>>> actionMap = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * Constructor
	 */
	public EditorEventManager() {
		super();
		dispatchThread.setName("Editor Event Thread");
	}
	
	/**
	 * Shutdown the even thread.
	 * 
	 */
	public void shutdown() {
		eventQueue.clear();
		dispatchTask.shutdown();
	
		// necessary to finish the 'take()' method of blocking queue
		dispatchThread.interrupt();
		
		actionMap.clear();
		eventQueue.clear();
	}
	
	/**
	 * Queue the given event.
	 * 
	 * @param ee
	 */
	public void queueEvent(EditorEvent<?> ee) {
		try {
			eventQueue.put(ee);
		} catch (InterruptedException e) {
			LogUtil.warning(e);
		}
		// start thread if necessary
		if (!dispatchThread.isAlive()) {
			dispatchThread.setFinishWhenQueueEmpty(true);
			dispatchThread.invokeLater(dispatchTask);
			dispatchThread.start();
		}
	}

	public <T> void registerActionForEvent(EditorEventType<T> eventType, EditorAction<T> action) {
		registerActionForEvent(eventType, action, RunOn.EditorEventDispatchThread);
	}

	/**
	 * Register a handler for the given event name
	 * 
	 * @param eventName
	 * @param action
	 * @param runOn
	 */
	public <T> void registerActionForEvent(EditorEventType<T> eventName, EditorAction<T> action, RunOn runOn) {
		synchronized (actionMap) {
			final EditorEventHandler<T> handler = new EditorEventHandler<>(action, runOn);
			List<EditorEventHandler<?>> handlers = actionMap.get(eventName);
			if(handlers == null) {
				handlers = new ArrayList<>();
				actionMap.put(eventName, handlers);
			}
			handlers.add(handler);
		}
	}
	
	/**
	 * Remove handler for the given event
	 * 
	 * @param eventType
	 * @param action
	 */
	public <T> void removeActionForEvent(EditorEventType<T> eventType, EditorAction<T> action) {
		synchronized (actionMap) {
			final List<EditorEventHandler<?>> handlers = actionMap.get(eventType);
			if(handlers != null) {
				final Optional<EditorEventHandler<?>> selectedHandler =
						handlers.stream().filter((h) -> h.action() == action).findAny();
				if(selectedHandler.isPresent())
					handlers.remove(selectedHandler);
			}
		}
	}
	
	/**
	 * Retrieve the list of actions for the specified event
	 * 
	 * @param eventName
	 * 
	 * @return list of handlers for the event
	 */
	private List<EditorEventHandler<?>> getHandlersForEvent(EditorEventType<?> eventName) {
		List<EditorEventHandler<?>> retVal = actionMap.get(eventName);
		if(retVal == null)
			retVal = new ArrayList<>();
		else
			retVal = Collections.unmodifiableList(retVal);
		return retVal;			
	}

	/**
	 * Task for dispatching events
	 */
	private final PhonTask dispatchTask = new PhonTask() {

		@Override
		public void performTask() {
			super.setStatus(TaskStatus.RUNNING);

			while(!isShutdown()) {
				EditorEvent<?> event = null;
				try {
					event = eventQueue.take();
				} catch (InterruptedException e) {
					if(!isShutdown()) {
						// an error only if we are not shutdown
						LogUtil.warning(e);
					}
				}

				if(event != null) {
					synchronized (actionMap) {
						if(PrefHelper.getBoolean("phon.debug", false)) {
							LogUtil.log(Level.TRACE, "Dispatching event: " + event.eventType());
							if(event.getData().isPresent())
								LogUtil.log(Level.TRACE, "\tdata: " + event.getData().get());
						}
						for (EditorEventHandler<?> action : getHandlersForEvent(event.eventType())) {
							action.handleEvent(event);
						}
					}
				}
			}

			super.setStatus(TaskStatus.FINISHED);
		}

	};

	public static enum RunOn {
		AWTEventDispatchThread,
		AWTEventDispatchThreadInvokeAndWait,
		EditorEventDispatchThread,
		BackgroundThread;
	}

	private record EditorEventHandler<T>(EditorAction<T> action, RunOn runOn) {

		public void handleEvent(EditorEvent<?> ee) {
			final Runnable doAction = () -> action.eventOccurred((EditorEvent<T>) ee);

			switch(runOn()) {
				case AWTEventDispatchThread -> {
					SwingUtilities.invokeLater(doAction);
				}

				case AWTEventDispatchThreadInvokeAndWait -> {
					try {
						SwingUtilities.invokeAndWait(doAction);
					} catch (InterruptedException | InvocationTargetException e) {
						LogUtil.warning(e);
					}
				}

				case BackgroundThread -> {
					PhonWorker.invokeOnNewWorker(doAction);
				}

				case EditorEventDispatchThread -> {
					doAction.run();
				}
			}
		}

	}

}
