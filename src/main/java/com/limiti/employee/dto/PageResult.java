package com.limiti.employee.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果封装
 *
 * <pre>
 * {
 *   "list": [ ... ],
 *   "total": 42,     // 总记录数
 *   "page": 1,       // 当前页码（从 1 开始）
 *   "size": 10,      // 每页条数
 *   "pages": 5       // 总页数
 * }
 * </pre>
 *
 * @author Limiti
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 总页数 */
    private int pages;

    public static <T> PageResult<T> of(Page<T> springPage) {
        PageResult<T> result = new PageResult<>();
        result.setList(springPage.getContent());
        result.setTotal(springPage.getTotalElements());
        result.setPage(springPage.getNumber() + 1);
        result.setSize(springPage.getSize());
        result.setPages(springPage.getTotalPages());
        return result;
    }
}
