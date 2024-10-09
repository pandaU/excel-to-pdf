package com.pandau.tools.pdftool.controller;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.zxing.WriterException;
import com.pandau.tools.pdftool.model.AjaxResult;
import com.pandau.tools.pdftool.model.BaseBatchOrder;
import com.pandau.tools.pdftool.model.SkuHandleConfigDTO;
import com.pandau.tools.pdftool.model.SkuHandleInfoDTO;
import com.pandau.tools.pdftool.repository.entity.SkuDetailInfo;
import com.pandau.tools.pdftool.repository.entity.SkuHandleInfo;
import com.pandau.tools.pdftool.repository.mapper.SkuDetailInfoMapper;
import com.pandau.tools.pdftool.repository.mapper.SkuHandleInfoMapper;
import com.pandau.tools.pdftool.service.SkuHandleInfoService;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Cleanup;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping({"/sku"})
public class UkPdfHandleController {
    private static final Logger log = LoggerFactory.getLogger(com.pandau.tools.pdftool.controller.UkPdfHandleController.class);

    @Autowired
    private SkuHandleInfoService skuHandleInfoService;

    @Autowired
    private SkuHandleInfoMapper skuHandleInfoMapper;

    @Autowired
    private SkuDetailInfoMapper skuDetailInfoMapper;

    @Value("${upload.path:/Users/yuwu/Public/to-pdf/file/}")
    private String uploadPath;

    @RequestMapping({"/index"})
    public String index() {
        return "manager/index";
    }

    @RequestMapping({"/skuManage"})
    public String skuPage() {
        return "manager/sku/skuList";
    }

    @RequestMapping({"/skuList"})
    @ResponseBody
    public Object skuList(Integer page, Integer limit, String fileName) {
        Page<SkuHandleInfo> infoPage = this.skuHandleInfoService.skuList(page, limit, fileName);
        Map<String, Object> rest = new HashMap<>();
        rest.put("code", Integer.valueOf(0));
        rest.put("msg", "");
        rest.put("count", Long.valueOf(infoPage.getTotal()));
        rest.put("data", infoPage.getRecords()
                .stream()
                .map(x -> modelToDto(x, infoPage.getRecords()))
                .collect(Collectors.toList()));
        return rest;
    }

    @RequestMapping({"/config"})
    public String config(Integer id, Model model) {
        SkuHandleInfo skuHandleInfo = (SkuHandleInfo) this.skuHandleInfoMapper.selectById(id);
        if (skuHandleInfo == null)
            throw new RuntimeException("数据不存在");
        String skuConfig = skuHandleInfo.getSkuConfig();
        SkuHandleConfigDTO configDTO = new SkuHandleConfigDTO();
        configDTO.setDpi(Integer.valueOf(203));
        configDTO.setWideSize(Integer.valueOf(50));
        configDTO.setLongSize(Integer.valueOf(30));
        configDTO.setFontSizeStr("r1=20,r2=18,r3=16,r4=16,r5=16");
        configDTO.setIsNeedQrCode("on");
        configDTO.setDesignStr("r1=c2,r2=c1,r3=c3,r4=c4,r5=c5");
        if (StringUtils.isNotBlank(skuConfig))
            configDTO = (SkuHandleConfigDTO) JSON.parseObject(skuConfig, SkuHandleConfigDTO.class);
        configDTO.setId(id);
        model.addAttribute("config", configDTO);
        return "manager/sku/skuConfig";
    }

    @RequestMapping({"/uploadFile"})
    public String config() {
        return "manager/sku/upload";
    }

    @RequestMapping({"/configEdit"})
    @ResponseBody
    public AjaxResult<SkuHandleInfo> configEdit(@RequestBody SkuHandleConfigDTO configDTO) {
        if (configDTO == null)
            throw new RuntimeException("数据不存在");
        SkuHandleInfo handleInfo = (SkuHandleInfo) this.skuHandleInfoMapper.selectById(configDTO.getId());
        handleInfo.setSkuConfig(JSON.toJSONString(configDTO));
        this.skuHandleInfoMapper.updateById(handleInfo);
        AjaxResult<SkuHandleInfo> result = new AjaxResult(true, "保存成功", handleInfo);
        return result;
    }

    private SkuHandleInfoDTO modelToDto(SkuHandleInfo skuHandleInfo, List<SkuHandleInfo> skuHandleInfos) {
        SkuHandleInfoDTO skuHandleInfoDTO = new SkuHandleInfoDTO();
        skuHandleInfoDTO.setId(skuHandleInfo.getId());
        skuHandleInfoDTO.setNo(Integer.valueOf(skuHandleInfos.indexOf(skuHandleInfo) + 1));
        skuHandleInfoDTO.setFileName(skuHandleInfo.getFileName());
        skuHandleInfoDTO.setFileId(skuHandleInfo.getFileId());
        skuHandleInfoDTO.setSkuNum(skuHandleInfo.getSkuNum());
        skuHandleInfoDTO.setSkuConfig(skuHandleInfo.getSkuConfig());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(skuHandleInfo.getUpdateTime().longValue());
        skuHandleInfoDTO.setUpdateTime(format.format(date));
        return skuHandleInfoDTO;
    }

    @RequestMapping(value = {"/upload"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object upload(@RequestParam("file") MultipartFile multipartFile) {
        if (multipartFile.isEmpty())
            throw new RuntimeException("文件不能为空");
        AjaxResult<Boolean> result = new AjaxResult();
        this.skuHandleInfoService.uploadExcel(multipartFile);
        result.ajaxTrue(Boolean.valueOf(true));
        return result;
    }

    @RequestMapping(value = {"/del"})
    @ResponseBody
    public Object del(BaseBatchOrder order) {
        AjaxResult<Boolean> result = new AjaxResult();
        result.ajaxTrue(Boolean.valueOf(true));
        if (order == null) {
            return result;
        }
        order.getIds().forEach(id -> {
            SkuHandleInfo skuHandleInfo = skuHandleInfoMapper.selectById(id);
            if (skuHandleInfo == null) {
                return;
            }
            String fileId = skuHandleInfo.getFileId();
            LambdaQueryWrapper<SkuDetailInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SkuDetailInfo::getFileId, fileId);
            skuDetailInfoMapper.delete(wrapper);
            skuHandleInfoMapper.deleteById(id);
        });

        return result;
    }

    @RequestMapping({"/preview"})
    public void preview(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response) throws IOException, WriterException {
        SkuHandleInfo handleInfo = (SkuHandleInfo) this.skuHandleInfoMapper.selectById(id);
        if (handleInfo == null)
            throw new RuntimeException("数据不存在");
        this.skuHandleInfoService.savePreviewFile(handleInfo);
        FileInputStream inputStream = new FileInputStream(this.uploadPath + handleInfo.getFileId() + ".pdf");
        response.setContentType("application/pdf; charset=utf-8");
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "private, must-revalidate");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "inline;filename=" +
                URLEncoder.encode(handleInfo.getFileId() + ".pdf", "UTF-8"));
        String errorMsg = null;
        int i = IOUtils.copy(inputStream, (OutputStream) response.getOutputStream());
        if (i > -1) {
            IOUtils.closeQuietly(new Closeable[]{inputStream, (Closeable) response.getOutputStream()});
            return;
        }
        errorMsg = "文件太大无法预览";
        try {
            response.setContentType("application/json; charset=utf-8");
            response.getOutputStream().write(JSON.toJSONBytes(errorMsg, new com.alibaba.fastjson.serializer.SerializerFeature[0]));
        } catch (IOException e) {
            log.error("错误信息写入失败", e);
        }
    }

    @RequestMapping({"/export"})
    public void export(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response) throws IOException, WriterException {
        SkuHandleInfo handleInfo = (SkuHandleInfo) this.skuHandleInfoMapper.selectById(id);
        if (handleInfo == null)
            throw new RuntimeException("数据不存在");
        this.skuHandleInfoService.savePreviewFile(handleInfo);
        FileInputStream inputStream = new FileInputStream(this.uploadPath + handleInfo.getFileId() + ".pdf");
        response.setContentType("application/octet-stream; charset=utf-8");
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "private, must-revalidate");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(handleInfo.getFileId() + ".pdf", "UTF-8"));
        String errorMsg = null;
        int i = IOUtils.copy(inputStream, (OutputStream) response.getOutputStream());
        if (i > -1) {
            IOUtils.closeQuietly(new Closeable[]{inputStream, (Closeable) response.getOutputStream()});
            return;
        }
        errorMsg = "文件太大无法下载";
        try {
            response.setContentType("application/json; charset=utf-8");
            response.getOutputStream().write(JSON.toJSONBytes(errorMsg, new com.alibaba.fastjson.serializer.SerializerFeature[0]));
        } catch (IOException e) {
            log.error("错误信息写入失败", e);
        }
    }

    @RequestMapping({"/batchExport"})
    @ResponseBody
    public Object batchExport(BaseBatchOrder order) throws IOException {
        if (order == null || CollectionUtils.isEmpty(order.getIds())) {
            throw new RuntimeException("未选择需导出的数据");
        }

        List<String> fileIds = Lists.newArrayList();
        order.getIds().forEach(id -> {
            SkuHandleInfo handleInfo = (SkuHandleInfo) this.skuHandleInfoMapper.selectById(id);
            if (handleInfo == null)
                return;
            try {
                this.skuHandleInfoService.savePreviewFile(handleInfo);
                fileIds.add(handleInfo.getFileId());
            } catch (Exception e) {
                log.error("数据导出失败", e);
            }
        });

        byte[] buffer = new byte[1024];
        String zipName = UUID.randomUUID().toString() + ".zip";
        String zipFile =this.uploadPath +  zipName;
        //文件压缩
        @Cleanup FileOutputStream fos = new FileOutputStream(zipFile);
        @Cleanup ZipOutputStream zos = new ZipOutputStream(fos);

        for (String fileId : fileIds) {
            FileInputStream inputStream = new FileInputStream(this.uploadPath +fileId + ".pdf");
            zos.putNextEntry(new ZipEntry(fileId + ".pdf"));
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                zos.write(buffer, 0,length);
            }
            zos.closeEntry();
            inputStream.close();
        }
        AjaxResult<String> result = new AjaxResult();
        result.ajaxTrue(zipName);
        return result;
    }


    @RequestMapping({"/download"})
    public void download(String zipFile, HttpServletRequest request, HttpServletResponse response) throws IOException {
        FileInputStream inputStream = new FileInputStream(this.uploadPath + zipFile);

        response.setContentType("application/octet-stream; charset=utf-8");
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "private, must-revalidate");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("批量导出.zip", "UTF-8"));
        String errorMsg = null;
        int i = IOUtils.copy(inputStream, (OutputStream) response.getOutputStream());
        if (i > -1) {
            IOUtils.closeQuietly(new Closeable[]{inputStream, (Closeable) response.getOutputStream()});
            File file = new File(this.uploadPath + zipFile);
            if (file.isFile()) {
                file.delete();
            }
            return;
        }
        errorMsg = "文件太大无法下载";
        try {
            response.setContentType("application/json; charset=utf-8");
            response.getOutputStream().write(JSON.toJSONBytes(errorMsg, new com.alibaba.fastjson.serializer.SerializerFeature[0]));
        } catch (IOException e) {
            log.error("错误信息写入失败", e);
        }
    }




}
