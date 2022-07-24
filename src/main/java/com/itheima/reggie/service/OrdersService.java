package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Orders;

import java.util.Map;

public interface OrdersService extends IService<Orders> {
    /**
     * 用户下单
     * @param orders
     */
    public void submit(Orders orders);

    /**
     * 再来一单
     * @param map
     */
    public void againSubmit(Map<String, String> map);
}
