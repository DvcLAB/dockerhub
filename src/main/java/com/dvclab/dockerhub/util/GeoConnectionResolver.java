package com.dvclab.dockerhub.util;

import one.rewind.nio.http.ReqObj;
import one.rewind.nio.http.Requester;
import one.rewind.nio.proxy.IpDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * IP转经纬度
 * 保存用户请求中的经纬度记录
 */
public class GeoConnectionResolver {

    public static final Logger logger = LogManager.getLogger(GeoConnectionResolver.class.getName());

    private static final String url_tpl = "http://api.ipstack.com/%s?access_key=67270fb47cce00b45bd94f18e5b139bc&format=1";

    private static final double[] local;

    // 运行时缓存，减少接口请求
    private static final Map<String, double[]> ip_geo_map = new HashMap<>();

    static {
        local = resolve(IpDetector.getIp());
    }

    /**
     * 根据ip获取经纬度
     * @param ip
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static double[] resolve(String ip) {

        return ip_geo_map.computeIfAbsent(ip, v -> {

            try {

                ReqObj r = Requester.req(String.format(url_tpl, ip)).get();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(r.rBody);

                double latitude = node.get("latitude").getDoubleValue();
                double longitude = node.get("longitude").getDoubleValue();

                // 临时设定
                if(latitude == 0 && longitude == 0) {
                    latitude = 34.50000;
                    longitude = 121.43333;
                }

                return new double[]{longitude, latitude};
            }
            catch (IOException e) {
                logger.error("Error get local address, ", e);
                return null;
            }
        });
    }
}
