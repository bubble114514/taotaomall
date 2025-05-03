package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
    @Autowired
    WareSkuService wareSkuService;

    /**
     * 1、库存自动解锁。
     *      下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的库存就要自动解锁。
     * 2、锁库存失败导致订单失败
     *
     * 只要解锁库存的消息失败，一定要告诉服务解锁失败
     *
     * @param to
     * @param msg
     */
    @RabbitHandler
    public void handleStockLocked(StockLockedTo to, Message msg, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息。。。");
        try {
            wareSkuService.unlockStock(to);
            // 执行成功，手动删除消息
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            // 拒绝消息，重新入队
            channel.basicReject(msg.getMessageProperties().getDeliveryTag(),true);
        }

    }
    @RabbitHandler
    public  void handleOrderCloseRelease(OrderTo to, Message msg, Channel channel) throws IOException {
        System.out.println("收到订单关闭，准备解锁库存。。。");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(msg.getMessageProperties().getDeliveryTag(),true);
        }
    }

}
