const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const imagesDir = './yolov11-master/data/images';

// 获取所有图片文件
const imageFiles = fs.readdirSync(imagesDir).filter(file =>
    file.endsWith('.jpg') || file.endsWith('.png') || file.endsWith('.webp')
);

console.log(`找到 ${imageFiles.length} 张图片，开始批量识别...\n`);

imageFiles.forEach((file, index) => {
    try {
        const filePath = path.join(imagesDir, file);

        console.log(`[${index + 1}/${imageFiles.length}] 正在识别: ${file}`);

        // 使用 curl 发送 POST 请求
        const result = execSync(
            `curl -s -X POST -F "file=@${filePath}" http://localhost:8081/upload`,
            { encoding: 'utf-8', cwd: __dirname }
        );

        try {
            const data = JSON.parse(result);
            console.log(`  ✅ 识别结果: ${data.messages}`);
            console.log(`  🖼️  标注图片: ${data.imgs_url}`);
        } catch (e) {
            console.log(`  ❌ 识别失败: ${result.substring(0, 100)}`);
        }

        console.log();

        // 添加小延迟，避免请求过快
        if (index < imageFiles.length - 1) {
            execSync('sleep 0.5', { encoding: 'utf-8' });
        }
    } catch (error) {
        console.error(`  ❌ 请求失败: ${error.message}`);
        console.log('请确保服务器已启动: node shibie.js\n');
        process.exit(1);
    }
});

console.log('========================================');
console.log('✅ 批量识别完成！');
console.log(`📊 共处理 ${imageFiles.length} 张图片`);
console.log('========================================');
