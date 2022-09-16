package com.yunzhong.datat.application.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JacksonUtil {
	private static final Logger log = LoggerFactory.getLogger(JacksonUtil.class);

	private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
			.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.serializationInclusion(JsonInclude.Include.NON_NULL).build();

	/**
	 * 完成对象序列化为字符串
	 * 
	 * @param obj 源对象
	 * @param <T>
	 * @return
	 */
	public static <T> String obj2String(T obj) {
		if (obj == null) {
			return null;
		}
		try {
			return obj instanceof String ? (String) obj : OBJECT_MAPPER.writeValueAsString(obj);
		} catch (Exception e) {
			log.warn("Parse Object to String error", e);
			return null;
		}
	}

	/**
	 * 完成对象序列化为字符串，但是字符串会保证一定的结构性（提高可读性，增加字符串大小）
	 * 
	 * @param obj 源对象
	 * @param <T>
	 * @return
	 */
	public static <T> String obj2StringPretty(T obj) {
		if (obj == null) {
			return null;
		}
		try {
			return obj instanceof String ? (String) obj
					: OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (Exception e) {
			log.warn("Parse Object to String error", e);
			return null;
		}
	}

	/**
	 * 完成字符串反序列化为对象
	 * 
	 * @param str   源字符串
	 * @param clazz 目标对象的Class
	 * @param <T>
	 * @return
	 */
	public static <T> T string2Obj(String str, Class<T> clazz) {
		if (!StringUtils.hasLength(str) || clazz == null) {
			return null;
		}
		try {
			return (clazz == String.class) ? (T) str : OBJECT_MAPPER.readValue(str, clazz);
		} catch (IOException e) {
			log.warn("Parse String to Object error", e);
		}
		return null;
	}
}
