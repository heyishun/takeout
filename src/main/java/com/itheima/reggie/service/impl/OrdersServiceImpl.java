package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper,Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;
    /**
     * 用户下单
     * @param orders
     */
    @Transactional
    @Override
    public void submit(Orders orders) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> list = shoppingCartService.list();
        if(list==null||list.size()==0){
            throw new CustomException("购物车为空，不能下单");
        }
        //查询用户信息
        User user = userService.getById(userId);
        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook==null){
            throw new CustomException("用户地址信息不能为空，不能下单");
        }
        //下单，向订单表插入数据，一条
        long orderId = IdWorker.getId();//订单号
        AtomicInteger amount=new AtomicInteger(0);
        List<OrderDetail> orderDetails=list.stream().map((item)->{
            OrderDetail orderDetail=new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName()==null ? "":addressBook.getProvinceName())
                +(addressBook.getCityName()==null ? "":addressBook.getCityName())
                +(addressBook.getDistrictName()==null ? "":addressBook.getDistrictName())
                +(addressBook.getDetail()==null ? "":addressBook.getDetail()));
        this.save(orders);
        //明细表插入数据，多条
        orderDetailService.saveBatch(orderDetails);
        //清空购物车数据
        shoppingCartService.remove(queryWrapper);
    }

    /**
     * 再来一单
     * @param map
     */
    @Override
    public void againSubmit(Map<String, String> map) {
        String ids = map.get("id");
        long id = Long.parseLong(ids);
        LambdaQueryWrapper<OrderDetail> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId,id);
        //获取该订单对应的所有的订单明细表
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        //通过用户ID把原来的购物车给清空
        shoppingCartService.clean();
        //获取用户ID
        Long userID = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {
            //把从order表中和order_details表中获取到的数据赋值给这个购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userID);
            shoppingCart.setImage(item.getImage());
            Long dishID = item.getDishId();
            Long setmealID = item.getSetmealId();
            if (dishID != null) {
                //如果是菜品那就添加菜品的查询条件
                shoppingCart.setDishId(dishID);
            } else {
                //添加到购物车的是套餐
                shoppingCart.setSetmealId(setmealID);
            }
            shoppingCart.setName(item.getName());
            shoppingCart.setDishFlavor(item.getDishFlavor());
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //把携带数据的购物车批量插入购物车表  这个批量保存的方法要使用熟练！！！
        shoppingCartService.saveBatch(shoppingCartList);
    }
}
