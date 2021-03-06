package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.model.Project;
import com.github.zafarkhaja.semver.Version;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.zafarkhaja.semver.expr.CompositeExpression.Helper.eq;
import static one.rewind.nio.web.cache.Caches.ses;

/**
 * 镜像缓存
 */
public class ImageCache {

	public static long update_interval = 60;

	public static Map<String, Image> images = new HashMap<>();

	/**
	 *
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static void init() throws DBInitException, SQLException {

		Image.getAll(Image.class).forEach(image -> {
			images.put(image.name, image);
		});

		// 定期批量保存回数据库
		ses.scheduleWithFixedDelay(() -> {
			Image.batchUpsert(List.copyOf(images.values()), "UPDATE tags = VALUES(tags)");
		}, update_interval, update_interval, TimeUnit.SECONDS);
	}

	/**
	 * 搜索满足项目的镜像列表
	 * @param p
	 * @return
	 */
	public static List<Image> getImagesForProject(Project p) {

		return images.values().stream()
				.filter(i -> {
					// 镜像的依赖项列表 包含 项目的全部依赖项名称
					if(i.libs!= null && p.deps.keySet().stream().allMatch(dep -> i.libs.containsKey(dep))) {
						// 版本号匹配
						return p.deps.entrySet().stream().allMatch(en ->
								Version.valueOf(i.libs.get(en.getKey())).satisfies(eq(en.getValue()))
						);
					}
					else {
						return false;
					}
				})
				.collect(Collectors.toList());
	}
}
