package com.atguigu.gulimallcart.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private  CartService cartService;


    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     *  浏览器有一个cookie：user-key。标识用户身份，一个月过期
     *  如果第一次使用jd的购物车功能，都会给一个临时的用户身份
     *  浏览器以后保存，每次发请求，都会带上这个cookie
     *
     *  用户登录：session里有
     *  没登录：按照cookie里面带来的user-key来做
     *  第一次：如果没有临时用户，擦护功能键一个临时用户
     */
    @GetMapping({"/cart.html", "/"})
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        //1、快速得到用户信息，id,user-key

        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     *
     * RedirectAttributes ra
     * RedirectAttributes ：重定向也可以保留数据
     *      1）、addAttribute()：session保存一份，重定向后还可以保留
     *      2）、addFlashAttribute()：在重定向后，会丢失，只能重定向到当前请求的请求路径
     * @return
     */
    @GetMapping("/addCartItem")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId, num);
//        model.addAttribute("skuId",skuId);
        ra.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        //重定向到成功页面。再次查询购物车数据即可
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("checked") Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/cart.html";

    }
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";

    }
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";

    }



}
