package com.pandau.tools.pdftool.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SkuInfoExcelVO {
  @ExcelProperty({"编号"})
  private String skuNo;

  @ExcelProperty({"产品名称"})
  private String skuName;

  @ExcelProperty({"尺寸"})
  private String skuSize;

  @ExcelProperty({"数量"})
  private String num;

  @ExcelProperty({"完整品名"})
  private String fullSkuName;
}
