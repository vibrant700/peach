package com.huimantaoxiang.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 二进制数据转换工具
 * 将JSON数据转换为二进制格式，提高数据隐蔽性
 */
public class BinaryDataConverter {

    // 文件头魔数
    private static final int MAGIC_NUMBER = 0x50454348; // "PECH" in hex
    private static final short VERSION = 1;

    // 简单混淆密钥
    private static final int OBFUSCATION_KEY = 0x5A;

    /**
     * 桃子参数数据结构
     */
    public static class PeachBinaryData {
        public String code;
        public String name;
        public String grade;
        public float sweetness;
        public int maturity;
        public int weight;
        public int diameter;
        public float confidence;
        public int lightScore;

        public PeachBinaryData(String code, String name, String grade,
                             float sweetness, int maturity, int weight, int diameter,
                             float confidence, int lightScore) {
            this.code = code;
            this.name = name;
            this.grade = grade;
            this.sweetness = sweetness;
            this.maturity = maturity;
            this.weight = weight;
            this.diameter = diameter;
            this.confidence = confidence;
            this.lightScore = lightScore;
        }
    }

    /**
     * 将JSON转换为二进制文件
     */
    public static void convertJsonToBinary(String jsonFilePath, String binaryOutputPath) throws IOException {
        List<PeachBinaryData> dataList = readJsonData(jsonFilePath);
        writeBinaryData(dataList, binaryOutputPath);
        System.out.println("成功转换 " + dataList.size() + " 条数据到二进制文件");
    }

    /**
     * 读取JSON数据
     */
    private static List<PeachBinaryData> readJsonData(String jsonFilePath) throws IOException {
        List<PeachBinaryData> dataList = new ArrayList<>();

        // 这里简化处理，实际应该用JSON解析器
        // 为了演示，我们手动创建数据
        dataList.add(new PeachBinaryData("cx_aru", "北京27号", "特级果", 12.5f, 78, 235, 82, 95.2f, 92));
        dataList.add(new PeachBinaryData("wc_bqw", "晚熟白青纹", "一级果", 11.5f, 75, 220, 80, 93.0f, 88));
        dataList.add(new PeachBinaryData("wc_brw", "晚熟红纹", "特级果", 13.0f, 82, 240, 85, 94.5f, 90));
        dataList.add(new PeachBinaryData("wc_bpv", "晚熟粉紫", "一级果", 12.2f, 78, 225, 81, 92.8f, 87));
        dataList.add(new PeachBinaryData("z14_crw", "14号深红纹", "特级果", 13.5f, 85, 250, 87, 96.0f, 93));
        dataList.add(new PeachBinaryData("aoyou_bpw", "油桃粉白纹", "一级果", 11.8f, 76, 200, 75, 91.5f, 85));
        dataList.add(new PeachBinaryData("p2_bpv", "P2粉紫", "特级果", 12.8f, 80, 230, 83, 94.2f, 89));
        dataList.add(new PeachBinaryData("yp_apu", "叶黄红粉底", "一级果", 12.0f, 77, 215, 79, 92.5f, 86));
        dataList.add(new PeachBinaryData("90peach_crw", "90成熟深红纹", "特级果", 14.0f, 88, 260, 90, 97.5f, 95));
        dataList.add(new PeachBinaryData("90peach_bqv", "90成熟青紫纹", "特级果", 13.8f, 86, 255, 88, 96.8f, 94));
        dataList.add(new PeachBinaryData("85peach_aqu", "85成熟红青底", "一级果", 12.5f, 80, 235, 82, 93.5f, 88));
        dataList.add(new PeachBinaryData("85peach_brw", "85成熟红纹", "一级果", 12.3f, 79, 228, 81, 93.0f, 87));
        dataList.add(new PeachBinaryData("hyp_bpv", "红皮粉紫", "特级果", 13.2f, 83, 245, 85, 95.0f, 91));
        dataList.add(new PeachBinaryData("hmp_crw", "红肉深红纹", "特级果", 13.6f, 84, 250, 86, 96.2f, 92));
        dataList.add(new PeachBinaryData("hmo_apu", "红肉橙红粉底", "一级果", 12.9f, 81, 238, 84, 94.8f, 90));
        dataList.add(new PeachBinaryData("14peach_bqv", "14号桃青紫纹", "一级果", 12.1f, 78, 218, 80, 92.9f, 86));

        return dataList;
    }

    /**
     * 写入二进制数据文件
     */
    private static void writeBinaryData(List<PeachBinaryData> dataList, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // 写入文件头
            dos.writeInt(MAGIC_NUMBER);
            dos.writeShort(VERSION);
            dos.writeShort(dataList.size());

            // 写入每条数据
            for (PeachBinaryData data : dataList) {
                // 写入字符串（长度 + UTF-8字节）
                writeString(dos, data.code);
                writeString(dos, data.name);
                writeString(dos, data.grade);

                // 写入数值（使用简单混淆）
                dos.writeInt(obfuscateInt(data.maturity));
                dos.writeInt(obfuscateInt(data.weight));
                dos.writeInt(obfuscateInt(data.diameter));
                dos.writeInt(obfuscateInt(data.lightScore));

                // 浮点数转换为整数进行混淆
                dos.writeInt(obfuscateInt(Float.floatToIntBits(data.sweetness)));
                dos.writeInt(obfuscateInt(Float.floatToIntBits(data.confidence)));
            }
        }
    }

    /**
     * 写入字符串
     */
    private static void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            dos.writeShort(0);
        } else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(bytes.length);
            dos.write(bytes);
        }
    }

    /**
     * 简单的整数混淆
     */
    private static int obfuscateInt(int value) {
        return value ^ OBFUSCATION_KEY;
    }

    /**
     * 解混淆整数
     */
    private static int deobfuscateInt(int value) {
        return value ^ OBFUSCATION_KEY;
    }

    /**
     * 主函数：执行转换
     */
    public static void main(String[] args) {
        try {
            String jsonPath = "assets/data/peach_params.json";
            String binaryPath = "assets/data/peach_params.dat";

            convertJsonToBinary(jsonPath, binaryPath);
            System.out.println("二进制文件生成成功: " + binaryPath);

        } catch (Exception e) {
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}