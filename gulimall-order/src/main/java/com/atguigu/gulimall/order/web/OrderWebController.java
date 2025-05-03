package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmOrderData", confirmVo);
        return "confirm";
    }

    /**
     * 提交订单
     * @param submitVo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo,Model model, RedirectAttributes ra){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(submitVo);
        //创建订单，验令牌，验价格，锁库存，下订单，减库存，清空购物车

        //下单成功，跳转到支付页面
        try{
            model.addAttribute("submitOrderResp", responseVo);
            return "pay";
        }catch ( Exception e){
            if (e instanceof NoStockException) {
                String message = ((NoStockException) e).getMessage();
                ra.addFlashAttribute("msg", "下单失败" + message);

            }
            return "redirect:http://order.gulimall.com/toTrade";

        }

    }
}
