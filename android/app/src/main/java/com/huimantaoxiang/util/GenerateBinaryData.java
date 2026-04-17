package com.huimantaoxiang.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 生成二进制数据文件
 */
public class GenerateBinaryData {

    private static final int MAGIC_NUMBER = 0x50454348; // "PECH"
    private static final short VERSION = 1;
    private static final int OBFUSCATION_KEY = 0x5A;

    public static void main(String[] args) {
        try {
            generateBinaryFile();
            System.out.println("二进制文件生成成功！");
        } catch (Exception e) {
            System.err.println("生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateBinaryData(String code, String name, String grade,
                                          float sweetness, int maturity, int weight, int diameter,
                                          float confidence, int lightScore,
                                          DataOutputStream dos) throws IOException {
        // 写入字符串
        writeString(dos, code);
        writeString(dos, name);
        writeString(dos, grade);

        // 写入混淆后的数值
        dos.writeInt(obfuscate(maturity));
        dos.writeInt(obfuscate(weight));
        dos.writeInt(obfuscate(diameter));
        dos.writeInt(obfuscate(lightScore));
        dos.writeInt(obfuscate(Float.floatToIntBits(sweetness)));
        dos.writeInt(obfuscate(Float.floatToIntBits(confidence)));
    }

    private static void generateBinaryFile() throws IOException {
        String outputPath = "app/src/main/assets/data/peach_params.dat";
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputPath))) {

            // 文件头
            dos.writeInt(MAGIC_NUMBER);
            dos.writeShort(VERSION);
            dos.writeShort(16); // 16个品种

            // 写入每个品种的数据
            generateBinaryData("cx_aru", "北京27号", "特级果", 12.5f, 78, 235, 82, 95.2f, 92, dos);
            generateBinaryData("wc_bqw", "晚熟白青纹", "一级果", 11.5f, 75, 220, 80, 93.0f, 88, dos);
            generateBinaryData("wc_brw", "晚熟红纹", "特级果", 13.0f, 82, 240, 85, 94.5f, 90, dos);
            generateBinaryData("wc_bpv", "晚熟粉紫", "一级果", 12.2f, 78, 225, 81, 92.8f, 87, dos);
            generateBinaryData("z14_crw", "14号深红纹", "特级果", 13.5f, 85, 250, 87, 96.0f, 93, dos);
            generateBinaryData("aoyou_bpw", "油桃粉白纹", "一级果", 11.8f, 76, 200, 75, 91.5f, 85, dos);
            generateBinaryData("p2_bpv", "P2粉紫", "特级果", 12.8f, 80, 230, 83, 94.2f, 89, dos);
            generateBinaryData("yp_apu", "叶黄红粉底", "一级果", 12.0f, 77, 215, 79, 92.5f, 86, dos);
            generateBinaryData("90peach_crw", "90成熟深红纹", "特级果", 14.0f, 88, 260, 90, 97.5f, 95, dos);
            generateBinaryData("90peach_bqv", "90成熟青紫纹", "特级果", 13.8f, 86, 255, 88, 96.8f, 94, dos);
            generateBinaryData("85peach_aqu", "85成熟红青底", "一级果", 12.5f, 80, 235, 82, 93.5f, 88, dos);
            generateBinaryData("85peach_brw", "85成熟红纹", "一级果", 12.3f, 79, 228, 81, 93.0f, 87, dos);
            generateBinaryData("hyp_bpv", "红皮粉紫", "特级果", 13.2f, 83, 245, 85, 95.0f, 91, dos);
            generateBinaryData("hmp_crw", "红肉深红纹", "特级果", 13.6f, 84, 250, 86, 96.2f, 92, dos);
            generateBinaryData("hmo_apu", "红肉橙红粉底", "一级果", 12.9f, 81, 238, 84, 94.8f, 90, dos);
            generateBinaryData("14peach_bqv", "14号桃青紫纹", "一级果", 12.1f, 78, 218, 80, 92.9f, 86, dos);
        }
    }

    private static void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            dos.writeShort(0);
        } else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(bytes.length);
            dos.write(bytes);
        }
    }

    private static int obfuscate(int value) {
        return value ^ OBFUSCATION_KEY;
    }
}