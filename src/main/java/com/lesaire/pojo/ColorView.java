package com.lesaire.pojo;
import jakarta.validation.constraints.*;
/**
 * 页面配色信息，id 为稳定标识，name 为中文名称，hex 用于绘制预览。
 */
public record ColorView(String id,String name,String hex) {}
