package com.gentics.mesh.context;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Abstract class for internal action context.
 */
public abstract class AbstractInternalActionContext extends AbstractActionContext implements InternalActionContext {

	@Override
	public void send(RestModel restModel, HttpResponseStatus status) {
		send(JsonUtil.toJson(restModel), status);
	}

	@Override
	public <T> Handler<AsyncResult<T>> errorHandler() {
		Handler<AsyncResult<T>> handler = t -> {
			if (t.failed()) {
				fail(t.cause());
			}
		};
		return handler;
	}

	@Override
	public Release getRelease(Project project) {
		if (project == null) {
			project = getProject();
		}
		if (project == null) {
			// TODO i18n
			throw error(INTERNAL_SERVER_ERROR, "Cannot get release without a project");
		}

		Release release = null;

		String releaseNameOrUuid = getVersioningParameters().getRelease();
		if (!isEmpty(releaseNameOrUuid)) {
			release = project.getReleaseRoot().findByUuid(releaseNameOrUuid);
			if (release == null) {
				release = project.getReleaseRoot().findByName(releaseNameOrUuid);
			}
			if (release == null) {
				throw error(BAD_REQUEST, "release_error_not_found", releaseNameOrUuid);
			}
		} else {
			release = project.getLatestRelease();
		}

		return release;
	}

}