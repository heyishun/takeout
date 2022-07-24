package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shoppingCart")

public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据：{}",shoppingCart);
        //设置用户id，指定是那个用户的购物车数据
        Long aLong = BaseContext.getCurrentId();
        log.info("{}",aLong);
        shoppingCart.setUserId(aLong);
        //查询当前菜品或套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(ShoppingCart::getUserId,aLong);
        if(dishId!=null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
        if(cartServiceOne!=null) {
            //如果存在，就在原来数量基础上加1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number+1);
            shoppingCartService.updateById(cartServiceOne);

        }else{
            //如果不存在默认就是1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne=shoppingCart;
        }
            return R.success(cartServiceOne);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        log.info("查看购物车...");
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list();
        return R.success(list);
    }

    /**
     * 减少
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        //设置用户id，指定是那个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        //查询当前菜品或套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);
        if(dishId!=null){
            //减少到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            //减少到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
        Integer integer = cartServiceOne.getNumber();

        if(integer>0) {
            cartServiceOne.setNumber(integer-1);
            shoppingCartService.updateById(cartServiceOne);
        }else{
            cartServiceOne.setNumber(0);
            shoppingCartService.updateById(cartServiceOne);
        }
        return R.success(cartServiceOne);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping ("/clean")
    public R<String> clean(){
        shoppingCartService.clean();
        return R.success("清空购物车成功");
    }
}
