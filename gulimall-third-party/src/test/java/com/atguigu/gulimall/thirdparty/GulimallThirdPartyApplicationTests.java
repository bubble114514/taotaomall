package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSS;
import com.atguigu.gulimall.thirdparty.util.SMSUtil;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
class GulimallThirdPartyApplicationTests {
    @Autowired
    OSS ossClient;
    @Autowired
    SMSUtil smsUtil;

    @Test
    public void sendSms(){
        smsUtil.sendMessage("SMS_154950909", "18231978918", "{\"code\":\"1234\"}");

    }
    @Test
    public void testUpload() throws com.aliyuncs.exceptions.ClientException, IOException {

//        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-beijing.aliyuncs.com";
//
//        String accessKeyId="LTAI5t7Z63u5HDPSHTroQqV6";
//        String accessKeySecret ="oGeWrAjHAkYwgyfb1G9nBcxIDcrWfG";
//        //创建OSSClient实例
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        //上传文件流
        InputStream inputStream = Files.newInputStream(Paths.get("C:\\Users\\19039\\Pictures\\steam\\2358720_1424.jpg"));
        ossClient.putObject("gulimall-paopaozi", "hahaha.jpg", inputStream);

        //关闭OssClient实例。

        ossClient.shutdown();
        System.out.println("上传成功");

    }
    @Test
    void contextLoads() {
    }

}
