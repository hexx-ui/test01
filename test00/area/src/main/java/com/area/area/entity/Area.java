package com.area.area.entity;

import lombok.Data;

/**
 * @description: XXXXX
 * @author: liyue
 * @date: 2020/7/27 14:41
 */
@Data
public class Area {
    private Long area_id;
    private String area_name;
    private String priority;
    private String create_time;
    private String last_edit_time;
}
