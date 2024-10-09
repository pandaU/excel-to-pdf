package com.pandau.tools.pdftool.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pandau.tools.pdftool.repository.entity.SkuDetailInfo;
import com.pandau.tools.pdftool.repository.mapper.SkuDetailInfoMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SkuDetailInfoRepository extends ServiceImpl<SkuDetailInfoMapper, SkuDetailInfo> implements IService<SkuDetailInfo> {}
