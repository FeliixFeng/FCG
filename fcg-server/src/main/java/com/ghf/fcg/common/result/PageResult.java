package com.ghf.fcg.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(Page<?> page, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.size = page.getSize();
        return r;
    }
}
