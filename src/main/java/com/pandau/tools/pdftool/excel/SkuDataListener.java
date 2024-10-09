package com.pandau.tools.pdftool.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.pandau.tools.pdftool.repository.SkuDetailInfoRepository;
import com.pandau.tools.pdftool.repository.entity.SkuDetailInfo;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkuDataListener extends AnalysisEventListener<SkuInfoExcelVO> {
  private static final Logger log = LoggerFactory.getLogger(com.pandau.tools.pdftool.excel.SkuDataListener.class);

  private static final int BATCH_COUNT = 200;

  private List<SkuInfoExcelVO> cachedDataList = ListUtils.newArrayListWithExpectedSize(200);

  private final SkuDetailInfoRepository skuDetailInfoRepository;

  private final String fileId;

  private int no = 1;

  public SkuDataListener(SkuDetailInfoRepository skuDetailInfoRepository, String fileId) {
    this.skuDetailInfoRepository = skuDetailInfoRepository;
    this.fileId = fileId;
  }

    @Override
  public void invoke(SkuInfoExcelVO data, AnalysisContext context) {
    log.info("解析到一条数据:{}", JSON.toJSONString(data));
    if (StringUtils.isBlank(data.getSkuNo())) {
      log.warn("数据不完整:{}", JSON.toJSONString(data));
      return;
    }
    this.cachedDataList.add(data);
    if (this.cachedDataList.size() >= 200) {
      saveData();
      this.cachedDataList = ListUtils.newArrayListWithExpectedSize(200);
    }
  }

  @Override
  public void doAfterAllAnalysed(AnalysisContext context) {
    saveData();
    this.no = 1;
    log.info("所有数据解析完成！");
  }

  private void saveData() {
    log.info("{}条数据，开始存储数据库！", Integer.valueOf(this.cachedDataList.size()));
    List<SkuDetailInfo> detailInfos = Lists.newArrayList();
    this.cachedDataList.stream().forEach(x -> {
      List<SkuDetailInfo> infos = voToEntity(x);
      detailInfos.addAll(infos);
    });
    this.skuDetailInfoRepository.saveBatch(detailInfos);
    log.info("存储数据库成功！");
  }

  private List<SkuDetailInfo> voToEntity(SkuInfoExcelVO vo) {
    List<SkuDetailInfo> infos = Lists.newArrayList();
    try {
      Integer num = Integer.parseInt(vo.getNum().replace("个",""));
      String voNum = vo.getNum();
      for (int i = 1; i <= num ; i++) {
        SkuDetailInfo skuDetailInfo = new SkuDetailInfo();
        skuDetailInfo.setFileId(this.fileId);
        skuDetailInfo.setNo(Integer.valueOf(this.no++));
        vo.setNum(voNum + "(" + i + "/" + num + ")");
        skuDetailInfo.setSkuContext(JSON.toJSONString(vo));
        infos.add(skuDetailInfo);
      }
    } catch (NumberFormatException e) {
      log.error("数量字段不是数字");
      SkuDetailInfo skuDetailInfo = new SkuDetailInfo();
      skuDetailInfo.setFileId(this.fileId);
      skuDetailInfo.setNo(Integer.valueOf(this.no++));
      skuDetailInfo.setSkuContext(JSON.toJSONString(vo));
      infos.add(skuDetailInfo);
    }
    return infos;
  }
}
