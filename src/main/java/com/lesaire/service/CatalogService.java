package com.lesaire.service;
import com.lesaire.pojo.*;
import java.util.List;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;
/**
 * 目录业务接口，为展示、报价和下单提供统一的有效选配数据。
 */
public interface CatalogService {
    /**
     * 汇总前端所需的款式、面料、配色和选项加价。
     */
    CatalogView get();
    Quote quote(Customization customization);
    ProductView uploadImage(String styleCode, MultipartFile file);
    Path imageFile(String filename);
    /**
     * 取得有效款式的展示信息。
     */
    ProductView product(String code);
    /**
     * 取得有效面料的展示信息。
     */
    FabricView fabric(String code);
    /**
     * 取得可下单的款式实体。
     */
    ShirtStyle style(String code);
    /**
     * 取得可下单的面料实体。
     */
    FabricMaterial material(String code);
    /**
     * 取得当前目录实际提供的颜色。
     */
    /**
     * 按选项分组和值查找有效定制选项。
     */
    CustomizationOption option(String group,String value);
    /**
     * 校验选配组合，返回报价和订单快照所需的选项。
     */
    List<CustomizationOption> selectedOptions(Customization customization);
}
