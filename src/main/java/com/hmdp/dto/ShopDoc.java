package com.hmdp.dto;

import com.hmdp.entity.Shop;
import lombok.Data;

@Data
public class ShopDoc {
    private Long id;
    private String name;
    private Long typeId;
    private String images;
    private String area;
    private String address;
    private Double x;
    private Double y;
    private Long avgPrice;
    private Integer sold;
    private Integer comments;
    private Integer score;
    private String openHours;

    public ShopDoc() {
    }

    public ShopDoc(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.typeId = shop.getTypeId();
        this.images = shop.getImages();
        this.area = shop.getArea();
        this.address = shop.getAddress();
        this.x = shop.getX();
        this.y = shop.getY();
        this.avgPrice = shop.getAvgPrice();
        this.sold = shop.getSold();
        this.comments = shop.getComments();
        this.score = shop.getScore();
        this.openHours = shop.getOpenHours();
    }
}
