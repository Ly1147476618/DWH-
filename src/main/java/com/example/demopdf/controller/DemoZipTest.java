package com.example.demopdf.controller;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.MalformedInputException;
import java.util.*;

@Controller
public class DemoZipTest {

    @GetMapping("/")
    public String goUploadPage() {
        return "parseDocument";
    }

    @PostMapping("/parseDocument")
    @ResponseBody
    public String parseDocument(@RequestParam("file") MultipartFile file,@RequestParam(value = "param", required = false) Integer param,@RequestParam("photo") MultipartFile photo,@RequestParam("status") Integer status) throws Exception {
        try {
            // 项目根目录下的 uploads 文件夹
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 如果不存在则创建
            }

            try {
                // 保存上传的 zip 到临时文件
                File tempZip = new File(uploadDir, file.getOriginalFilename());
                file.transferTo(tempZip);
                // 根解压文件夹路径
                String rootFolderPath = uploadDir + tempZip.getName().replace(".zip", "");
                File rootFolder = new File(rootFolderPath);
                if (!rootFolder.exists()) rootFolder.mkdirs();

                try {
                    unzip(tempZip.getAbsolutePath(), rootFolderPath);
                    tempZip.delete(); // 删除临时 zip
                } catch (IOException e) {
                    e.printStackTrace();
                    return "解压失败：" + e.getMessage();
                }

                // 只返回解压后的根文件夹路径
                String absolutePath = rootFolder.getAbsolutePath();

                // 1.保存照片
                String photoName = photo.getOriginalFilename();
                File photoDest = new File(dir, photoName);
                photo.transferTo(photoDest);

                //************************************************
                // 📁 你的PDF文件夹
                // 🖼 本地照片路径
                String imagePath = photoDest.getAbsolutePath();

//                File[] files = folder.listFiles((dirPDF, name) -> name.toLowerCase().endsWith(".pdf"));
                File[] files = getAllPdfFiles(absolutePath);

                if (files == null || files.length == 0) {
                    System.out.println("文件夹中没有 PDF");
                    return "文件夹中没有 PDF";
                }

                // 获取当前用户桌面路径
                String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";

                String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

                // 在桌面创建 output 文件夹
                File outputFolder = new File(desktopPath + File.separator + "/" + shortUuid + "_output");
                if (!outputFolder.exists()) {
                    outputFolder.mkdirs(); // 不存在就创建
                }

                //处理pdf页码问题
                for (File pdfFile : files) {
                    processPdf(pdfFile, imagePath, outputFolder.getAbsolutePath(),param,status);
                }

                System.out.println("处理完成！");
                //************************************************

                //删除
                File DelUploadDir = new File(uploadDir);
                deleteFolderContents(DelUploadDir);

                return "--->>> 解压完成，保存路径：" + outputFolder;
            } catch (IOException e) {
                e.printStackTrace();
                return "上传或解压失败：" + e.getMessage();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "上传失败：" + e.getMessage();
        }
    }

    //获取解析后pdf文件数组
    public static File[] getAllPdfFiles(String absolutePath) {
        File folder = new File(absolutePath);
        List<File> pdfList = new ArrayList<>();
        collectPdfFiles(folder, pdfList);

        // 转成 File[]
        return pdfList.toArray(new File[0]);
    }

    // 递归方法
    private static void collectPdfFiles(File folder, List<File> pdfList) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectPdfFiles(file, pdfList); // 递归子目录
            } else if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                pdfList.add(file);
            }
        }
    }

    //删除服务器临时文件
    private static void deleteFolderContents(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                deleteFolderContents(file); // 递归删除子目录
            }
            // 先尝试删除，如果失败，可多尝试几次
            boolean deleted = file.delete();
            int attempts = 5;
            while (!deleted && attempts-- > 0) {
                try {
                    Thread.sleep(50); // 等50ms再尝试
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                deleted = file.delete();
            }
            if (!deleted) {
                System.err.println("删除失败：" + file.getAbsolutePath());
            }
        }
    }

    //pdf删除指定
    private static void processPdf(File pdfFile, String imagePath, String outputFolder,Integer param,Integer status) throws Exception {
        try (PDDocument document = PDDocument.load(pdfFile)) {

            int originalPages = document.getNumberOfPages();

            // 删除到第几页--------↓↓-------- (没办法属实Nb)
            if (originalPages <= param) {
                System.out.println(pdfFile.getName() + " 页数不足 "+ param+"页，跳过");
                return;
            }

            // 删除前 11 页（从大到小删除）
            for (int i = param; i > 0; i--) {
                document.removePage(i);
            }

            // 处理剩余的每一页，贴图
            PDImageXObject image = PDImageXObject.createFromFile(imagePath, document);


            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);

                PDRectangle box = page.getMediaBox();
                float pageWidth = box.getWidth();
                float pageHeight = box.getHeight();

                // 读取页面有效宽高
                PDRectangle mb = page.getMediaBox();
                int rotation = page.getRotation();

                if(status == 2){
                    page.setRotation((rotation + 270) % 360);
                }

                float imgWidth = 0;
                float imgHeight = 0;
                float x = 0;
                float y = 0;

                try (PDPageContentStream cs = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true)) {

                    //底部
                    if(status == 0){

                        imgWidth = 509.20f;  // 图片宽度，可按需调整
                        imgHeight = 90; // 图片高度

                        x = 58.14F;          // 距离左边 30px，可调
                        y = 31;          // 距离底部 30px，可调

                    //右侧
                    }else if (status == 1){

                        //***********************************************右下角
                        //图片宽高
                        //float imgWidth = 343.35f;   // 可自行调整
                        imgWidth = 343.15f;   // 可自行调整
                        imgHeight = 60;

                        //移动图片xy轴
                        x = 455.5f;
                        //float y = 36;  // 离底部 20px，可调
                        y = 37;  // 离底部 20px，可调

                        //1.开启操作图片
                        cs.saveGraphicsState();

                        //2.绘制图片
                        // 🔥 按旋转角度进行强制解算
                        switch(rotation) {
                            case 90:
                                cs.transform(new Matrix(0, 1, -1, 0, mb.getWidth(), 0));
                                break;
                            case 180:
                                cs.transform(new Matrix(-1, 0, 0, -1, mb.getWidth(), mb.getHeight()));
                                break;
                            case 270:
                                cs.transform(new Matrix(0, -1, 1, 0, 0, mb.getHeight()));
                                break;
                            default:
                                // rotation = 0，保持不变
                                break;
                        }

                        //***********************************************右下角
                    // 先旋转90° 再将图片防止到右下角
                    } else if (status == 2) {
                        imgWidth = 509.20f;  // 图片宽度，可按需调整
                        imgHeight = 90; // 图片高度

                        x = 20F;          // 距离左边 30px，可调
                        y = 20F;          // 距离底部 30px，可调

                        cs.saveGraphicsState();

                        // 顺时针旋转 90°，旋转中心在图片左下角
                        Matrix rotation2 = Matrix.getRotateInstance(Math.toRadians(270), x, y);
                        cs.transform(rotation2);
                    }

                    //3.保存绘制图片
                    if(status == 2){
                        x = -524;
                        y = 13;
                    }
                    cs.drawImage(image, x, y, imgWidth, imgHeight);
                    //4.完全恢复坐标系
                    cs.restoreGraphicsState();
                }
            }

            String outPath = outputFolder + "/" + pdfFile.getName();
            document.save(outPath);
            System.out.println("已处理: " + pdfFile.getName());
        }
    }

    private static final String[] ENCODINGS = {
            "UTF-8",   // Linux / Mac 常用
            "GBK",     // 中国 Windows 常用
            "MS932"    // 日本 Windows 常用（Shift-JIS）
    };

    /**
     * 解压 zip 文件到指定目录
     */
    private List<String> unzip(String zipFilePath, String destDir) throws IOException {
        for (String encoding : ENCODINGS) {
            try {
                return unzipWithEncoding(zipFilePath, destDir, encoding);
            } catch (Exception ignored) {
                // 自动尝试下一个编码
            }
        }
        throw new IOException("无法解析 ZIP 文件名编码");
    }

    private List<String> unzipWithEncoding(String zipFilePath, String destDir, String encoding) throws IOException {
        List<String> folderPaths = new ArrayList<>();

        try (InputStream fis = new FileInputStream(zipFilePath);
             ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis, encoding, false)) {

            ZipArchiveEntry entry;
            while ((entry = (ZipArchiveEntry) zipIn.getNextEntry()) != null) {
                String path = destDir + File.separator + entry.getName();

                if (entry.isDirectory()) {
                    File dir = new File(path);
                    if (!dir.exists()) dir.mkdirs();
//                    folderPaths.add(dir.getAbsolutePath()); // 只记录文件夹路径
                } else {
                    // 文件，确保父目录存在
                    File parent = new File(path).getParentFile();
                    if (!parent.exists()) parent.mkdirs();

                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = zipIn.read(buffer)) != -1) {
                            bos.write(buffer, 0, read);
                        }
                    }

                    // 记录文件父目录
                    File parentDir = new File(path).getParentFile();
                    folderPaths.add(parentDir.getAbsolutePath());
                }
            }
        }

        // 去重
        return new ArrayList<>(new HashSet<>(folderPaths));
    }

}
