package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Image;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ImageCache extends Caches{

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
			Image.batchUpsert(new ArrayList(images.values()), "UPDATE tags = VALUES(tags)");
		}, update_interval, update_interval, TimeUnit.SECONDS);
	}
}
