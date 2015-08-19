package com.letv.cdn.openapi.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lichao
 * Date: 11-8-14
 * Time: 下午1:40
 * To change this template use File | Settings | File Templates.
 */
public class MapUtil<T, K> {

    private Map<T, K> map = new HashMap<T, K>();

    public static <T, K> MapUtil<T, K> create() {
        return new MapUtil<T, K>();
    }

    public static <T, K> MapUtil<T, K> create(T t, K k) {
        MapUtil<T, K> tkMapUtil = create();
        tkMapUtil.put(t, k);
        return tkMapUtil;
    }

    public MapUtil<T, K> put(T t, K k) {
        map.put(t, k);
        return this;
    }

    public Map<T, K> getMap() {
        return map;
    }

    public static Map getMap(Object... args) {
        if (args.length % 2 != 0) throw new RuntimeException("需偶数个参数");
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    public static String getString(Map map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v != null) {
            return v.toString();
        } else {
            return defaultValue;
        }
    }


    public static int getInt(Map map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v != null) {
            try {
                return Integer.parseInt(v.toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }


    public static long getLong(Map map, String key, long defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v != null) {
            try {
                return Long.parseLong(v.toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }
}
