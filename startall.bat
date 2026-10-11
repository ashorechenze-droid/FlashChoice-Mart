@echo off
chcp 65001 >nul
echo ============================================================
echo  启动所有 FCM 微服务（带 SkyWalking 探针）
echo  项目路径：D:\idea_project\FlashChoice-Mart
echo ============================================================
echo.

REM ========== 基础变量（已按您的实际路径配置）==========
set AGENT_PATH=D:\SkyWalking-Apache\java-agent\skywalking-agent\skywalking-agent.jar
set BACKEND_SERVICE=localhost:11800
set PROJECT_ROOT=D:\idea_project\FlashChoice-Mart
set MICRO_ROOT=%PROJECT_ROOT%\FCM-microservice
REM =======================================================

REM ========== 各服务 jar 包路径 ===========================
REM ★★★ 重要：请将下面的版本号（如 1.0.0）替换为您实际打包出的版本号 ★★★
REM 如果您的 jar 包没有版本号（如直接叫 FCM-brand-microservice.jar），请删除 "-1.0.0"
REM 您可以在每个模块的 target 目录下查看实际文件名

set BRAND_JAR=%MICRO_ROOT%\FCM-brand-microservice\target\FCM-brand-microservice-1.0.0.jar
set CART_JAR=%MICRO_ROOT%\FCM-cart-microservice\target\FCM-cart-microservice-1.0.0.jar
set COLLECT_JAR=%MICRO_ROOT%\FCM-collect-microservice\target\FCM-collect-microservice-1.0.0.jar
set CATEGORY_JAR=%MICRO_ROOT%\FCM-category-microservice\target\FCM-category-microservice-1.0.0.jar
set GOOD_JAR=%MICRO_ROOT%\FCM-good-microservice\target\FCM-good-microservice-1.0.0.jar
set MEMBER_ADDRESS_JAR=%MICRO_ROOT%\FCM-member-address-microservice\target\FCM-member-address-microservice-1.0.0.jar
set MEMBER_JAR=%MICRO_ROOT%\FCM-member-microservice\target\FCM-member-microservice-1.0.0.jar
set ORDER_JAR=%MICRO_ROOT%\FCM-order-microservice\target\FCM-order-microservice-1.0.0.jar
set RBAC_JAR=%MICRO_ROOT%\FCM-rbac-microservice\target\FCM-rbac-microservice-1.0.0.jar
set REGION_JAR=%MICRO_ROOT%\FCM-region-microservice\target\FCM-region-microservice-1.0.0.jar
set SECKILL_JAR=%MICRO_ROOT%\FCM-seckill-microservice\target\FCM-seckill-microservice-1.0.0.jar
set UPLOAD_JAR=%MICRO_ROOT%\FCM-upload-microservice\target\FCM-upload-microservice-1.0.0.jar
set USER_JAR=%MICRO_ROOT%\FCM-user-microservice\target\FCM-user-microservice-1.0.0.jar
set GATEWAY_JAR=%PROJECT_ROOT%\FCM-gateway\target\FCM-gateway-1.0.0.jar
REM =======================================================

REM ========== 启动所有服务（每个服务在新窗口运行）==========
start "brand-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=brand-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%BRAND_JAR%"
start "cart-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=cart-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%CART_JAR%"
start "collect-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=collect-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%COLLECT_JAR%"
start "category-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=category-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%CATEGORY_JAR%"
start "good-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=good-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%GOOD_JAR%"
start "member-address-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=member-address-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%MEMBER_ADDRESS_JAR%"
start "member-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=member-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%MEMBER_JAR%"
start "order-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=order-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%ORDER_JAR%"
start "rbac-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=rbac-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%RBAC_JAR%"
start "region-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=region-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%REGION_JAR%"
start "seckill-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=seckill-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%SECKILL_JAR%"
start "upload-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=upload-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%UPLOAD_JAR%"
start "user-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=user-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%USER_JAR%"
start "gateway-api" java -javaagent:"%AGENT_PATH%" -Dskywalking.agent.service_name=gateway-api -Dskywalking.collector.backend_service=%BACKEND_SERVICE% -jar "%GATEWAY_JAR%"
REM =======================================================

echo.
echo ============================================================
echo  所有服务已启动完成！
echo  请检查各个命令行窗口，确认无报错信息。
echo  网关地址：http://localhost:8888
echo ============================================================
pause