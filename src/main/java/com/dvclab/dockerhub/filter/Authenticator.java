package com.dvclab.dockerhub.filter;

import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.User;
import com.sun.jersey.api.NotFoundException;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.ProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Token 验证以及生成
 *
 */
public class Authenticator implements Filter {

	private static final Logger logger = LogManager.getLogger(Authenticator.class);
	private final static String AUTHORIZATION = "Authorization";
	private final static String AUTHORIZATION_PREFIX = "Bearer ";

	public Authenticator() {}

	/**
	 * 获取http请求中的token
	 *
	 * @param request request
	 * @return newToken
	 * @throws NotFoundException NotFoundException
	 */
	public static String getToken(Request request) throws ProtocolException {

		if (!request.headers().contains(AUTHORIZATION)) {

			throw new ProtocolException();
		}

		final String authorizationHeader = request.headers(AUTHORIZATION);

		if (!authorizationHeader.contains(AUTHORIZATION_PREFIX)) {
			logger.info(request.requestMethod());
			throw new ProtocolException();
		}

		// First index will contain an empty string
		return authorizationHeader.split(AUTHORIZATION_PREFIX)[1];
	}

	/**
	 * 验证Token
	 *
	 * @param q  request
	 * @param a response
	 * @throws ProtocolException ProtocolException
	 */
	@Override
	public void handle(Request q, Response a) throws ProtocolException, IOException, URISyntaxException, DBInitException, SQLException, ModelException.ClassNotEqual, IllegalAccessException {

		String token = getToken(q);

		User user = UserCache.TOKEN_USERS.get(token);

		if(user == null) {
			Pair<User, Date> verified_info = KeycloakAdapter.getInstance().verifyAccessToken(token);

			user = UserCache.update(token, verified_info.getLeft(), verified_info.getRight());
		}

		q.session().attribute("uid", user.id);
		q.session().attribute("roles", user.roles);
	}
}
