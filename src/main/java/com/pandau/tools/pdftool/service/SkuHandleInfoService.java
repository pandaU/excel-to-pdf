package com.pandau.tools.pdftool.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.pandau.tools.pdftool.excel.SkuDataListener;
import com.pandau.tools.pdftool.excel.SkuInfoExcelVO;
import com.pandau.tools.pdftool.model.SkuHandleConfigDTO;
import com.pandau.tools.pdftool.repository.SkuDetailInfoRepository;
import com.pandau.tools.pdftool.repository.entity.SkuDetailInfo;
import com.pandau.tools.pdftool.repository.entity.SkuHandleInfo;
import com.pandau.tools.pdftool.repository.mapper.SkuHandleInfoMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SkuHandleInfoService {
  private static final float CUSTOM_POINTS_PER_MM = 0.03937008F;

  @Autowired
  private SkuDetailInfoRepository skuDetailInfoRepository;

  @Autowired
  private SkuHandleInfoMapper skuHandleInfoMapper;

  @Value("${upload.path:/Users/yuwu/Public/to-pdf/file/}")
  private String uploadPath;

  @Transactional(rollbackFor = {Exception.class})
  public void uploadExcel(MultipartFile file) {
    try {
      InputStream fileInputStream = file.getInputStream();
      String fileId = UUID.randomUUID().toString();
      EasyExcel.read(fileInputStream, SkuInfoExcelVO.class, (ReadListener)new SkuDataListener(this.skuDetailInfoRepository, fileId)).sheet().doRead();
      LambdaQueryWrapper<SkuDetailInfo> wrapper = new LambdaQueryWrapper();
      wrapper.eq(SkuDetailInfo::getFileId, fileId);
      long skuNum = this.skuDetailInfoRepository.count((Wrapper)wrapper);
      SkuHandleInfo skuHandleInfo = new SkuHandleInfo();
      skuHandleInfo.setFileName(file.getOriginalFilename());
      skuHandleInfo.setFileId(fileId);
      skuHandleInfo.setUpdateTime(Long.valueOf(System.currentTimeMillis()));
      skuHandleInfo.setSkuNum(String.valueOf(skuNum));
      SkuHandleConfigDTO configDTO = new SkuHandleConfigDTO();
      configDTO.setDpi(Integer.valueOf(203));
      configDTO.setWideSize(Integer.valueOf(50));
      configDTO.setLongSize(Integer.valueOf(30));
      configDTO.setFontSizeStr("r1=20,r2=18,r3=16,r4=16,r5=16");
      configDTO.setDesignStr("r1=c2,r2=c1,r3=c3,r4=c4,r5=c5");
      configDTO.setIsNeedQrCode("on");
      skuHandleInfo.setSkuConfig(JSON.toJSONString(configDTO));
      this.skuHandleInfoMapper.insert(skuHandleInfo);
    } catch (IOException e) {
      throw new RuntimeException("文件上传失败");
    }
  }

  public Page<SkuHandleInfo> skuList(Integer currentPage, Integer size, String fileName) {
    Page<SkuHandleInfo> page = new Page();
    page.setCurrent(currentPage.intValue());
    page.setSize(size.intValue());
    LambdaQueryWrapper<SkuHandleInfo> wrapper = new LambdaQueryWrapper();
    wrapper.orderByDesc(SkuHandleInfo::getId);
    if (StringUtils.isNotBlank(fileName))
      wrapper.like(SkuHandleInfo::getFileName, fileName);
    Page<SkuHandleInfo> infoPage = (Page<SkuHandleInfo>)this.skuHandleInfoMapper.selectPage((IPage)page, (Wrapper)wrapper);
    return infoPage;
  }

  public void savePreviewFile(SkuHandleInfo handleInfo) throws IOException, WriterException {
    String skuConfig = handleInfo.getSkuConfig();
    if (StringUtils.isBlank(skuConfig))
      throw new RuntimeException("配置为空, 请先配置");
    String fileId = handleInfo.getFileId();
    LambdaQueryWrapper<SkuDetailInfo> wrapper = new LambdaQueryWrapper();
    wrapper.eq(SkuDetailInfo::getFileId, fileId);
    wrapper.orderByAsc(SkuDetailInfo::getNo);
    List<SkuDetailInfo> detailInfos = this.skuDetailInfoRepository.list((Wrapper)wrapper);
    SkuHandleConfigDTO configDTO = (SkuHandleConfigDTO)JSON.parseObject(skuConfig, SkuHandleConfigDTO.class);
    Integer longSize = configDTO.getLongSize();
    Integer wideSize = configDTO.getWideSize();
    Integer dpi = configDTO.getDpi();
    String fontSizeStr = configDTO.getFontSizeStr();
    Map<String, String> fontConfigMap = strToMap(fontSizeStr);
    if (StringUtils.isBlank(fontSizeStr))
      throw new RuntimeException("字体大小为空, 请先配置");
    PDDocument doc = new PDDocument();
    InputStream fontFile = getClass().getResourceAsStream("/fonts/SimHei.ttf");
    PDType0Font simheiFont = PDType0Font.load(doc, fontFile);
    for (SkuDetailInfo detailInfo : detailInfos) {
      String skuContext = detailInfo.getSkuContext();
      SkuInfoExcelVO infoExcelVO = (SkuInfoExcelVO)JSON.parseObject(skuContext, SkuInfoExcelVO.class);
      PDRectangle rectangle = new PDRectangle(wideSize.intValue() * 0.03937008F * dpi.intValue(), longSize.intValue() * 0.03937008F * dpi.intValue());
      PDPage page = new PDPage(rectangle);
      doc.addPage(page);
      PDPageContentStream stream = new PDPageContentStream(doc, page);
      float widthMargin = 10.0F;
      float highMargin = 80.0F;
      float y = 0.0F;
      Map<String, String> designMap = new HashMap<>();
      List<String> designStrList = Lists.newArrayList();
      int i;
      for (i = 1; i <= 5; i++) {
        String context, fontSize = fontConfigMap.get("r" + i);
        stream.setFont((PDFont)simheiFont, Integer.parseInt(fontSize));
        if (i == 1) {
          context = infoExcelVO.getSkuNo();
        } else if (i == 2) {
          context = infoExcelVO.getSkuName();
        } else if (i == 3) {
          context = infoExcelVO.getSkuSize();
        } else if (i == 4){
          context = infoExcelVO.getNum();
        } else {
          context = infoExcelVO.getFullSkuName();
        }
        float textWidth = simheiFont.getStringWidth(context) / 1000.0F * Integer.parseInt(fontSize);
        if (textWidth > 200.0F) {
          List<String> maxWidthString = getMaxWidthString(context, Integer.valueOf(Integer.parseInt(fontSize)), simheiFont);
          designStrList.addAll(maxWidthString);
          for (String s : maxWidthString)
            designMap.put(s, fontSize);
        } else {
          designStrList.add(context);
          designMap.put(context, fontSize);
        }
      }
      for (i = 1; i <= designStrList.size(); i++) {
        String context = designStrList.get(i - 1);
        String fontSize = designMap.get(context);
        stream.setFont((PDFont)simheiFont, Integer.parseInt(fontSize));
        float textWidth = simheiFont.getStringWidth(context) / 1000.0F * Integer.parseInt(fontSize);
        float pageWidth = page.getMediaBox().getWidth() - widthMargin;
        float x = pageWidth - textWidth - widthMargin;
        if (i == 1) {
          y = page.getMediaBox().getHeight() - highMargin;
        } else {
          y -= (Integer.parseInt(fontSize) + 10);
        }
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(context);
        stream.endText();
      }
      String isNeedQrCode = configDTO.getIsNeedQrCode();
      if ("on".equalsIgnoreCase(isNeedQrCode)) {
        String qrCodeText = infoExcelVO.getSkuNo();
        File tempFile = generateQRCodeImage(qrCodeText);
        PDImageXObject qrCodeImage = LosslessFactory.createFromImage(doc, ImageIO.read(tempFile));
        stream.drawImage(qrCodeImage, 0.0F, 30.0F, qrCodeImage.getWidth(), qrCodeImage.getHeight());
      }
      stream.close();
    }
    doc.save(this.uploadPath + fileId + ".pdf");
    doc.close();
    deleteTmpFile(this.uploadPath + "tmp");
  }

  public static void deleteTmpFile(String dirPath) {
    File directory = new File(dirPath);
    if (directory.exists() && directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null)
        for (File file : files) {
          if (file.isFile())
            file.delete();
        }
    }
  }

  private List<String> getMaxWidthString(String cxt, Integer fontSize, PDType0Font simheiFont) throws IOException {
    int length = cxt.length();
    for (int i = 10; i <= length; i++) {
      String substring = cxt.substring(0, i);
      float width = simheiFont.getStringWidth(substring) / 1000.0F * fontSize.intValue();
      if (width > 200.0F) {
        ArrayList<String> list = Lists.newArrayList( cxt.substring(0, i));
        if (StringUtils.isNotBlank(cxt.substring(i)))
          list.addAll(getMaxWidthString(cxt.substring(i), fontSize, simheiFont));
        return list;
      }
    }
    return Lists.newArrayList(cxt);
  }

  private File generateQRCodeImage(String qrCodeText) throws WriterException, IOException {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 150, 150);
    Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
    File tempFile = File.createTempFile("qrcode", ".png", new File(this.uploadPath + "tmp"));
    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", tempFile.toPath());
    return tempFile;
  }

  private Map<String, String> strToMap(String configStr) {
    List<String> configs = Lists.newArrayList(configStr.split(","));
    Map<String, String> map = new HashMap<>();
    for (String s : configs) {
      String[] split1 = s.split("=");
      map.put(split1[0], split1[1]);
    }
    return map;
  }
}