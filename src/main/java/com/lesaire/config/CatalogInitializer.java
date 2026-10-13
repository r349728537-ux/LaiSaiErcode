package com.lesaire.config;
import com.lesaire.mapper.CustomizationOptionMapper;
import com.lesaire.mapper.FabricMapper;
import com.lesaire.mapper.ShirtStyleMapper;
import com.lesaire.pojo.CustomizationOption;
import com.lesaire.pojo.FabricMaterial;
import com.lesaire.pojo.ShirtStyle;
import com.lesaire.utils.CatalogUtils;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 启动时补充空目录和缺失的应用选项，保留已有款式、面料和选项记录。 */
@Component
public class CatalogInitializer implements CommandLineRunner {
    private final ShirtStyleMapper styles;
    private final FabricMapper fabrics;
    private final CustomizationOptionMapper options;
    public CatalogInitializer(ShirtStyleMapper styles,FabricMapper fabrics,CustomizationOptionMapper options) {
        this.styles=styles;this.fabrics=fabrics;this.options=options;
    }
    /**
     * 启动时补齐空目录的演示款式、面料，以及当前表单缺少的定制选项。
     */
    @Override @Transactional public void run(String... args) {
        // 仅在款式表为空时创建演示款式，保留已有目录。
        if(styles.count()==0) {
            ShirtStyle coffee=new ShirtStyle("RS-CF-001","咖啡厅短袖莱赛尔衬衫","咖啡厅 / 轻餐","适合咖啡厅与轻餐门店的日常服务制服。",198);
            coffee.setSleeveType("short");coffee.setCollarType("shirt");styles.insert(coffee);
            ShirtStyle hotel=new ShirtStyle("RS-HO-002","酒店西餐厅长袖衬衫","酒店 / 西餐厅","适合酒店及西餐厅前厅服务。",258);
            hotel.setSleeveType("long");hotel.setCollarType("shirt");styles.insert(hotel);
            ShirtStyle bakery=new ShirtStyle("RS-BK-003","烘焙工坊立领衬衫","烘焙 / 甜品","适合烘焙工坊与甜品门店。",228);
            bakery.setSleeveType("short");bakery.setCollarType("stand");styles.insert(bakery);
        }
        // 仅在面料表为空时创建演示面料。
        if(fabrics.count()==0) {
            fabrics.insert(new FabricMaterial("LY-TW-001","莱赛尔斜纹抗皱布","莱赛尔 65% / 棉 30% / 氨纶 5%","暖白","#F7F2E8",135,0));
            fabrics.insert(new FabricMaterial("LY-BL-002","莱赛尔细斜纹布","莱赛尔 70% / 聚酯纤维 30%","深海蓝","#193A5B",145,18));
            fabrics.insert(new FabricMaterial("LY-GR-003","莱赛尔天丝混纺","莱赛尔 80% / 棉 20%","鼠尾草绿","#98A68E",150,25));
        }
        add("FIT","regular","自然合身",0);add("FIT","relaxed","宽松自在",0);
        add("LENGTH","standard","标准长度",0);add("LENGTH","long","加长款",30);
        add("EMBROIDERY","false","无刺绣",0);add("EMBROIDERY","true","枝叶刺绣",40);
        for(FabricMaterial f:fabrics.findActive()) add("COLOR",CatalogUtils.colorId(f.getColorHex()),f.getColorName(),0);
    }
    /**
     * 以业务编码判断选项是否已存在，初始化时只补充缺失项。
     */
    private void add(String group,String value,String name,int price) {
        String code=CatalogUtils.optionCode(group,value);
        if(!options.existsByCode(code)) options.insert(new CustomizationOption(code,group,name,value,price,0));
    }
}
