const express = require('express');
const router = express.Router();
const app = express();
const fs = require('fs')
const os = require('os');
var bodyParser = require('body-parser');
const cors = require('cors');
const multer = require('multer');

app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));
app.use('/yolov5-master/data/images', express.static('./yolov5-master/data/images'))
app.use('/yolov5-master/runs', express.static('./yolov5-master/runs'))

// 动态获取本机IP地址
function getLocalIP() {
    const interfaces = os.networkInterfaces();
    for (let name of Object.keys(interfaces)) {
        for (let iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return 'localhost';
}

const LOCAL_IP = getLocalIP();
console.log("检测到本机IP:", LOCAL_IP);

let upload = multer({
    storage: multer.diskStorage({
        destination: function (req, file, cb) {
            cb(null, './yolov5-master/data/images');
        },
        filename: function (req, file, cb) {
            var changedName = (file.originalname);
            cb(null, changedName);
        }
    })
})

// 上传并识别接口
app.post('/upload', upload.single('file'), function(req, res, next) {
    let file = req.file;
    let d0 = "";

    const spawn = require("child_process").spawn;
    const path = require("path");

    // 使用绝对路径
    const pythonPath = path.join(__dirname, "yolov5-master", ".venv", "Scripts", "python.exe");
    const detectScript = path.join(__dirname, "yolov5-master", "detect.py");
    const weightsPath = path.join(__dirname, "yolov5-master", "best.pt");
    const sourcePath = path.join(__dirname, "yolov5-master", "data", "images", file.filename);

    console.log("Python路径:", pythonPath);
    console.log("检测脚本:", detectScript);
    console.log("图片路径:", sourcePath);

    // 直接调用Python，不通过cmd.exe
    const result = spawn(pythonPath, [detectScript, "--weights", weightsPath, "--source", sourcePath], {
        env: { ...process.env, PYTHONIOENCODING: 'utf-8' }
    });

    result.stdout.on("data", (data) => {
        d0 += data.toString('utf8');
    });

    let errorOutput = "";
    result.stderr.on("data", (data) => {
        errorOutput += data.toString('utf8');
    });

    result.on("close", (code) => {
        console.log("child process exited with code " + code);
        
        // 解析 YOLOv5 输出，提取识别结果
         // 典型输出: image 1/1 .../upload.jpg: 640x640 1 cx_aru, 568.6ms
         // 或者: image 1/1 .../upload.jpg: 480x640 1 cx_aru, Done. (0.016s)
         
         // 尝试匹配 640x640 之后的内容
         const lineRegex = /image \d+\/\d+ .+: \d+x\d+ (.+?)(?:, Done\.|\, \d+\.?\d*ms)/;
         const lineMatch = errorOutput.match(lineRegex);
         
         let detectResult = "未识别到目标";
         if (lineMatch && lineMatch[1]) {
             // lineMatch[1] 可能是 "1 cx_aru"
             let resultPart = lineMatch[1].trim();
             
             // 如果有逗号分隔多个结果，取第一个
             let items = resultPart.split(", ");
             if (items.length > 0) {
                 let firstItem = items[0]; 
                 // 去掉开头的数字 (例如 "1 cx_aru" -> "cx_aru")
                 let parts = firstItem.split(" ");
                 if (parts.length > 1 && !isNaN(parts[0])) {
                     detectResult = parts.slice(1).join(" ");
                 } else {
                     detectResult = firstItem;
                 }
             }
         } else {
            // 备用：尝试匹配最后出现的单词（非符号）
            console.log("未匹配到标准行，尝试备用解析...");
            const words = errorOutput.match(/(\b\w+\b)/g);
            if (words && words.length > 0) {
                 // 这里很难保证准确，但可以作为最后的尝试
                 // 通常 Done 是最后的单词之一
            }
        }
        
        console.log("原始输出片段:", errorOutput.substring(errorOutput.indexOf("image 1/1") > 0 ? errorOutput.indexOf("image 1/1") : 0));
        console.log("解析结果:", detectResult);

        if (code !== 0) {
            return res.status(500).json({
                msg: "error",
                error: errorOutput
            });
        }

        // 动态获取最新生成的 exp 目录
        const expDirs = fs.readdirSync("./yolov5-master/runs/detect")
                          .filter(dir => dir.startsWith("exp"))
                          .sort((a, b) => b.localeCompare(a));
        const latestExp = expDirs[0];

        // 发送完整响应 - 使用动态IP
        res.status(200).json({
            msg: "success",
            imgs_url: `http://${LOCAL_IP}:8081/yolov5-master/runs/detect/${latestExp}/${file.filename}`,
            messages: detectResult
        });
    });
});

app.get('/', (req, res)=>{
    res.send('ok')
})

const server = app.listen(8081, function () {
    var host = server.address().address
    var port = server.address().port
    console.log("==========================================");
    console.log("  桃子识别服务已启动");
    console.log("  本机IP: " + LOCAL_IP);
    console.log("  访问地址为 http://" + LOCAL_IP + ":" + port);
    console.log("  上传接口: http://" + LOCAL_IP + ":" + port + "/upload");
    console.log("==========================================");
})
