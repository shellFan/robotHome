package com.robot.home.robot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单位归一化服务
 * <p>
 * 用于参数对比时将不同单位的数值归一化到基准单位，以便比较大小。
 * 例如：g → kg, mm → m, km/h → m/s, min → h
 * <p>
 * unitGroup 定义了同一类参数的单位集合，归一化到组内基准单位。
 */
@Service
public class UnitConversionService {

    private static final Logger log = LoggerFactory.getLogger(UnitConversionService.class);

    /**
     * 单位组 → (单位 → 归一化到基准单位的系数)
     * 系数含义：1 {单位} = 系数 × {基准单位}
     */
    private static final Map<String, Map<String, BigDecimal>> UNIT_GROUPS = new HashMap<>();

    static {
        // 重量组：基准 kg
        Map<String, BigDecimal> weight = new HashMap<>();
        weight.put("kg", BigDecimal.ONE);
        weight.put("g", new BigDecimal("0.001"));
        weight.put("t", new BigDecimal("1000"));
        weight.put("mg", new BigDecimal("0.000001"));
        UNIT_GROUPS.put("weight", Collections.unmodifiableMap(weight));

        // 长度组：基准 m
        Map<String, BigDecimal> length = new HashMap<>();
        length.put("m", BigDecimal.ONE);
        length.put("mm", new BigDecimal("0.001"));
        length.put("cm", new BigDecimal("0.01"));
        length.put("km", new BigDecimal("1000"));
        length.put("dm", new BigDecimal("0.1"));
        UNIT_GROUPS.put("length", Collections.unmodifiableMap(length));

        // 速度组：基准 m/s
        Map<String, BigDecimal> speed = new HashMap<>();
        speed.put("m/s", BigDecimal.ONE);
        speed.put("km/h", new BigDecimal("0.277778"));
        speed.put("cm/s", new BigDecimal("0.01"));
        speed.put("mm/s", new BigDecimal("0.001"));
        UNIT_GROUPS.put("speed", Collections.unmodifiableMap(speed));

        // 时间组：基准 h
        Map<String, BigDecimal> time = new HashMap<>();
        time.put("h", BigDecimal.ONE);
        time.put("min", new BigDecimal("0.016667"));
        time.put("s", new BigDecimal("0.000278"));
        time.put("ms", new BigDecimal("0.000000278"));
        UNIT_GROUPS.put("time", Collections.unmodifiableMap(time));

        // 功率组：基准 W
        Map<String, BigDecimal> power = new HashMap<>();
        power.put("W", BigDecimal.ONE);
        power.put("kW", new BigDecimal("1000"));
        power.put("MW", new BigDecimal("1000000"));
        power.put("HP", new BigDecimal("745.7"));
        UNIT_GROUPS.put("power", Collections.unmodifiableMap(power));

        // 压力组：基准 Pa
        Map<String, BigDecimal> pressure = new HashMap<>();
        pressure.put("Pa", BigDecimal.ONE);
        pressure.put("kPa", new BigDecimal("1000"));
        pressure.put("MPa", new BigDecimal("1000000"));
        UNIT_GROUPS.put("pressure", Collections.unmodifiableMap(pressure));

        // 电压组：基准 V
        Map<String, BigDecimal> voltage = new HashMap<>();
        voltage.put("V", BigDecimal.ONE);
        voltage.put("kV", new BigDecimal("1000"));
        voltage.put("mV", new BigDecimal("0.001"));
        UNIT_GROUPS.put("voltage", Collections.unmodifiableMap(voltage));

        // 电流组：基准 A
        Map<String, BigDecimal> current = new HashMap<>();
        current.put("A", BigDecimal.ONE);
        current.put("mA", new BigDecimal("0.001"));
        current.put("kA", new BigDecimal("1000"));
        UNIT_GROUPS.put("current", Collections.unmodifiableMap(current));

        // 容量组：基准 Ah
        Map<String, BigDecimal> capacity = new HashMap<>();
        capacity.put("Ah", BigDecimal.ONE);
        capacity.put("mAh", new BigDecimal("0.001"));
        capacity.put("Wh", BigDecimal.ONE); // 近似
        UNIT_GROUPS.put("capacity", Collections.unmodifiableMap(capacity));

        // 扭矩组：基准 N·m
        Map<String, BigDecimal> torque = new HashMap<>();
        torque.put("N·m", BigDecimal.ONE);
        torque.put("N·cm", new BigDecimal("0.01"));
        torque.put("kgf·cm", new BigDecimal("0.0980665"));
        UNIT_GROUPS.put("torque", Collections.unmodifiableMap(torque));
    }

    /**
     * 将数值从指定单位归一化到基准单位
     *
     * @param valueStr 数值字符串（如 "1.5"）
     * @param unit     原始单位（如 "km/h"）
     * @param unitGroup 单位组（如 "speed"）
     * @return 归一化后的数值，无法转换时返回 null
     */
    public BigDecimal normalize(String valueStr, String unit, String unitGroup) {
        if (valueStr == null || unit == null || unitGroup == null) {
            return null;
        }
        try {
            // 提取数值部分（去掉非数字后缀）
            String numStr = valueStr.trim().replaceAll("[^0-9.\\-]", "");
            if (numStr.isEmpty()) {
                return null;
            }
            BigDecimal value = new BigDecimal(numStr);
            Map<String, BigDecimal> group = UNIT_GROUPS.get(unitGroup);
            if (group == null) {
                return null;
            }
            BigDecimal factor = group.get(unit);
            if (factor == null) {
                return null;
            }
            return value.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.debug("单位归一化数值解析失败: value={}, unit={}, unitGroup={}", valueStr, unit, unitGroup);
            return null;
        }
    }

    /**
     * 判断单位组是否支持归一化
     */
    public boolean isSupported(String unitGroup) {
        return unitGroup != null && UNIT_GROUPS.containsKey(unitGroup);
    }
}