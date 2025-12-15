package com.llmplatform.vo;

import lombok.Data;

import java.util.List;

/**
 * VO for paginated learning records response
 */
@Data
public class LearningRecordsVO {

    /**
     * List of learning records
     */
    private List<LearningRecordVO> records;

    /**
     * Total number of records
     */
    private Long total;

    /**
     * Current page number
     */
    private Integer page;

    /**
     * Page size
     */
    private Integer pageSize;

    /**
     * Total number of pages
     */
    private Integer totalPages;
}
