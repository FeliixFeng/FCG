package com.ghf.fcg.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基础参数
 */
@Data
public class PageQuery {

    @Schema(description = "页码，默认1")
    private long page = 1;

    @Schema(description = "每页条数，默认20")
    private long size = 20;
}
