package com.pandau.tools.pdftool.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("sku_detail_info")
@Data
public class SkuDetailInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @TableField("file_id")
    private String fileId;

    @TableField("no")
    private Integer no;

    @TableField("sku_context")
    private String skuContext;

}
