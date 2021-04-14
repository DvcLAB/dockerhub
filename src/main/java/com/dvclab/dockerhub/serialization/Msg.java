package com.dvclab.dockerhub.serialization;

import com.google.common.collect.ImmutableMap;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.List;
import java.util.Map;

/**
 * 响应状态信息与状态码
 * @author lw
 */
public class Msg<T> implements JSONable<Msg> {

	public enum Code {

		// 成功
		SUCCESS(1, "SUCCESS"),

		// 失败
		FAILURE(0, "FAILURE"),

		//无效参数
		INVALID_PARAMETERS(213, "INVALID_PARAMETERS"),

		// DB错误
		// 创建失败
		INSERT_FAILURE(221, "INSERT_FAILURE"),

		// 更新失败
		UPDATE_FAILURE(222, "UPDATE_FAILURE"),

		// 删除失败
		DELETE_FAILURE(223, "DELETE_FAILURE"),

		// 错误请求
		BAD_REQUEST(400, "BAD_REQUEST"),

		// 无权访问
		TOKEN_INVALID(401, "TOKEN_INVALID"),

		// 匿名访问
		ACCESS_ANONYMOUS(402, "ACCESS_ANONYMOUS"),

		// 权限受限
		ACCESS_DENIED(403, "ACCESS_DENIED"),

		// 不存在
		NOT_FOUND(404, "NOT_FOUND"),

		// 拒绝访问
		METHOD_REJECTED(405, "METHOD_REJECTED"),

		// 请求过于频繁
		TOO_MANGY_REQ(406, "TOO_MANGY_REQ"),

		// 已经存在
		OBJECT_EXITS(409, "OBJECT_EXITS"),

		// 服务器错误
		SERVER_ERROR(500, "SERVER_ERROR");

		public Integer code;
		public String msg;

		Code(Integer code, String msg) {
			this.code = code;
			this.msg = msg;
		}
	}

	private int code;

	private String msg;

	private T data;

	public Map<String, Long> _meta;

	private Msg() {}

	/**
	 *
	 * @param code
	 * @param data
	 * @param _meta
	 */
	public Msg(Code code, T data, Map<String, Long> _meta) {
		this.code = code.code;
		this.msg = code.msg;
		this.data = data;
		this._meta = _meta;
	}

	/**
	 *
	 * @return
	 */
	public static Msg success() {
		return new Msg(Code.SUCCESS, null, null);
	}

	/**
	 *
	 * @param data
	 * @param <T>
	 * @return
	 */
	public static <T> Msg<T> success(T data) {

		return new Msg(Code.SUCCESS, data, null);
	}

	/**
	 *
	 * @param data
	 * @param total
	 * @param <T>
	 * @return
	 */
	public static <T extends List> Msg<T> success(T data, long total) {

		return new Msg(Code.SUCCESS, data, ImmutableMap.of("total", total));
	}

	/**
	 * success(返回数据和分页信息)
	 *
	 * @param data  返回数据
	 * @param size  size
	 * @param page  page
	 * @param total total
	 * @return restMsg
	 */
	public static Msg success(List data, long size, long page, long total) {

		long total_page = (page == 0 || size == 0 || total == 0) ? 0 : total / size + (total % size > 0 ? 1 : 0);

		return new Msg(Code.SUCCESS, data, ImmutableMap.of("page", page, "size", size, "total", total, "total_page", total_page));
	}

	/**
	 * Failure(默认)
	 *
	 * @return restMsg
	 */
	public static Msg failure() {
		return new Msg(Code.FAILURE, null, null);
	}


	/**
	 * failure(返回异常或错误信息)
	 *
	 * @param t Exception or Error
	 * @return restMsg
	 */
	public static Msg failure(Throwable t) {

		String msg = t.getClass().getName() + (t.getMessage() != null ? ("::" + t.getMessage()) : "");
		return new Msg(Code.FAILURE, msg, null);
	}

	/**
	 * failure(返回自定义错误信息)
	 *
	 * @param msg 错误信息
	 * @return restMsg
	 */
	public static Msg failure(String msg) {
		return new Msg(Code.FAILURE, msg != null ? msg : "FAILURE", null);
	}

	/**
	 * toJson
	 *
	 * @return json
	 */
	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}