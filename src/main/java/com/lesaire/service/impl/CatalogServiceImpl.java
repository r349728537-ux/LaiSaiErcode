package com.lesaire.service.impl;

import com.lesaire.mapper.CustomizationOptionMapper;
import com.lesaire.mapper.FabricMapper;
import com.lesaire.mapper.ShirtStyleMapper;
import com.lesaire.pojo.*;
import com.lesaire.service.CatalogService;
import com.lesaire.utils.CatalogUtils;
import com.lesaire.utils.ImageStorage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 目录、选配校验、报价和图片上传集中在一个业务实现类。 */
@Service
public class CatalogServiceImpl implements CatalogService {
    private final ShirtStyleMapper styles;
    private final FabricMapper fabrics;
    private final CustomizationOptionMapper options;
    private final ImageStorage images;

    public CatalogServiceImpl(ShirtStyleMapper styles, FabricMapper fabrics,
                              CustomizationOptionMapper options, ImageStorage images) {
        this.styles = styles;
        this.fabrics = fabrics;
        this.options = options;
        this.images = images;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogView get() {
        List<FabricMaterial> materials = fabrics.findActive();
        List<ColorView> colors = colors(materials);
        String defaultColor = colors.isEmpty() ? "cloud" : colors.get(0).id();
        List<ProductView> products = new ArrayList<>();
        List<FabricView> fabricViews = new ArrayList<>();
        for (ShirtStyle style : styles.findActive()) products.add(productView(style, defaultColor));
        for (FabricMaterial fabric : materials) fabricViews.add(fabricView(fabric));
        OptionPrices prices = new OptionPrices(option("LENGTH", "long").getPriceDelta(),
                                               option("EMBROIDERY", "true").getPriceDelta());
        return new CatalogView(products, fabricViews, colors, prices);
    }

    // 单价 = 款式价 + 面料加价 + 选项加价，总价 = 单价 × 数量。
    @Override
    @Transactional(readOnly = true)
    public Quote quote(Customization selection) {
        BigDecimal base = style(selection.productId()).getBasePrice();
        BigDecimal fabric = material(selection.fabricId()).getSurcharge();
        BigDecimal detail = BigDecimal.ZERO;
        for (CustomizationOption option : selectedOptions(selection)) {
            detail = detail.add(option.getPriceDelta());
        }
        BigDecimal unit = base.add(fabric).add(detail);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(selection.quantity()));
        return new Quote(base, fabric, detail, unit, selection.quantity(), total, "预计 7–12 个工作日制作完成");
    }

    @Override
    public ProductView uploadImage(String styleCode, MultipartFile file) {
        style(styleCode);
        String filename = images.save(file);
        boolean linked = false;
        try {
            // 单条更新保存地址，文件读写不放在数据库事务中。
            if (styles.updateImageUrl(styleCode, "/uploads/styles/" + filename) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "款式已停用，请刷新后重试");
            }
            linked = true;
            return product(styleCode);
        } finally {
            if (!linked) images.delete(filename);
        }
    }

    @Override
    public Path imageFile(String filename) {
        return images.find(filename);
    }

    public ShirtStyle style(String code) {
        ShirtStyle style = styles.findActiveByCode(code);
        if (style == null) throw bad("款式不存在或已停用，请重新选择");
        return style;
    }

    public FabricMaterial material(String code) {
        FabricMaterial fabric = fabrics.findActiveByCode(code);
        if (fabric == null) throw bad("面料不存在或已停用，请重新选择");
        return fabric;
    }

    public CustomizationOption option(String group, String value) {
        CustomizationOption option = options.findActiveByCode(CatalogUtils.optionCode(group, value));
        if (option == null) throw bad("此定制选项已停用，请重新选择");
        return option;
    }

    public List<CustomizationOption> selectedOptions(Customization selection) {
        String fabricColor = CatalogUtils.colorId(material(selection.fabricId()).getColorHex());
        if (!fabricColor.equals(selection.colorId())) throw bad("面料与配色不一致，请重新选择面料");
        return List.of(option("FIT", selection.fit()), option("LENGTH", selection.length()),
            option("EMBROIDERY", selection.embroidery().toString()), option("COLOR", selection.colorId()));
    }

    public ProductView product(String code) {
        return productView(style(code), "cloud");
    }

    public FabricView fabric(String code) {
        return fabricView(material(code));
    }

    private ProductView productView(ShirtStyle style, String color) {
        return new ProductView(style.getCode(), style.getName(), style.getScene(), style.getDescription(),
            style.getBasePrice(), color, style.getScene(), "shirt", style.getPreviewSleeve(),
            style.getPreviewCollar(), style.getImageUrl());
    }

    private FabricView fabricView(FabricMaterial fabric) {
        String weight = fabric.getWeight() == null ? "未标注克重" : fabric.getWeight() + " g/m²";
        return new FabricView(fabric.getCode(), fabric.getName(), fabric.getComposition(),
            fabric.getColorName() + "配色，" + weight + "。", fabric.getSurcharge(), weight,
            CatalogUtils.colorId(fabric.getColorHex()));
    }

    private List<ColorView> colors(List<FabricMaterial> materials) {
        Map<String, ColorView> result = new LinkedHashMap<>();
        for (FabricMaterial fabric : materials) {
            String id = CatalogUtils.colorId(fabric.getColorHex());
            result.putIfAbsent(id, new ColorView(id, fabric.getColorName(), fabric.getColorHex()));
        }
        return new ArrayList<>(result.values());
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
