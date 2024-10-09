package com.pandau.tools.pdftool.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("sku_handle_info")
@Data
public class SkuHandleInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("file_name")
    private String fileName;

    @TableField("file_id")
    private String fileId;

    @TableField("sku_num")
    private String skuNum;

    @TableField("sku_config")
    private String skuConfig;

    @TableField("update_time")
    private Long updateTime;
}
