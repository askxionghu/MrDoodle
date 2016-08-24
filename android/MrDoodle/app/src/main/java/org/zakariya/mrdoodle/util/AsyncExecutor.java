package org.zakariya.mrdoodle.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by shamyl on 8/22/16.
 */
public class AsyncExecutor {

	public interface Job<T> {
		T execute() throws Exception;
	}

	public interface JobListener<T> {
		void onComplete(T result);

		void onError(Throwable error);
	}

	private class JobInfo<T> {
		Future future;
		List<JobListener<T>> listeners = new ArrayList<>();
	}

	private Handler mainThreadHandler;
	private ExecutorService threadPool;
	private final Map<String, JobInfo> jobsById = new HashMap<>();

	public AsyncExecutor() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		mainThreadHandler = new Handler(Looper.getMainLooper());
	}

	public <T> void execute(final String id, final Job<T> job, JobListener<T> listener) {
		synchronized (jobsById) {
			if (jobsById.containsKey(id)) {
				@SuppressWarnings("unchecked")
				JobInfo<T> info = jobsById.get(id);
				info.listeners.add(listener);
			} else {
				final JobInfo<T> info = new JobInfo<>();
				jobsById.put(id, info);

				info.listeners.add(listener);
				info.future = threadPool.submit(new Runnable() {
					@Override
					public void run() {
						try {
							dispatchResult(info, job.execute());
						} catch (Exception e) {
							dispatchError(info, e);
						}
						synchronized (jobsById) {
							jobsById.remove(id);
						}
					}
				});
			}
		}
	}

	public boolean isScheduled(String id) {
		synchronized (jobsById) {
			JobInfo info = jobsById.get(id);
			return info != null && !info.future.isDone() && !info.future.isCancelled();
		}
	}

	public boolean cancel(String id) {
		synchronized (jobsById) {
			if (jobsById.containsKey(id)) {
				JobInfo info = jobsById.get(id);
				info.listeners.clear();
				Future future = info.future;
				if (!future.isCancelled() && !future.isDone()) {
					return future.cancel(false);
				}
			}
			return false;
		}
	}

	private <T> void dispatchResult(final JobInfo<T> info, final T result) {
		synchronized (jobsById) {
			mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					for (JobListener<T> listener : info.listeners) {
						listener.onComplete(result);
					}
				}
			});
		}
	}

	private <T> void dispatchError(final JobInfo<T> info, final Exception e) {
		synchronized (jobsById) {
			mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					for (JobListener<T> listener : info.listeners) {
						listener.onError(e);
					}
				}
			});
		}
	}
}
