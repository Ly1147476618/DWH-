package com.example.demopdf.controller;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;

public class DeomTest {

    public static void main() {
        String inputPath = "C:/Users/15051/Desktop/130102_AA0_0083-设计预算和图纸-富天大厦.pdf";    // 原 PDF
        String outputPath = "C:/Users/15051/Desktop/130102_AA0_0083-设计预算和图纸-富天大厦.pdf";  // 删除后输出 PDF

        int deleteCount = 11; // 要删除前 11 页

        try (PDDocument document = PDDocument.load(new File(inputPath))) {

            int totalPage = document.getNumberOfPages();
            System.out.println("总页数: " + totalPage);

            // 防止越界
            deleteCount = Math.min(deleteCount, totalPage);

            // PDFBox 页码是 index，从 0 开始
            for (int i = 0; i < deleteCount; i++) {
                document.removePage(0); // 每次删第 1 页（索引 0）
            }

            // 保存
            document.save(outputPath);
            System.out.println("完成！已删除前 " + deleteCount + " 页，输出文件：" + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

    }




}
