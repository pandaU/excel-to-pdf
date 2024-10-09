package com.pandau.tools.pdftool.model;

import lombok.Data;

@Data
public class SkuHandleConfigDTO {
    private Integer id;

    private Integer longSize;

    private Integer wideSize;

    private Integer exportNum;

    private String fontSizeStr;

    private String designStr;

    private String isNeedQrCode;

    private Integer dpi;


}
