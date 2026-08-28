# Awake_DW release 混淆补充规则。
# 默认沿用 proguard-android-optimize.txt；Hilt/Room/Compose 的 keep 规则
# 均由各自构件以 consumer rules 下发，此处只放本项目确需的补充。
# 目前无需额外 keep——若 minify 后出现运行期缺失类，先在此最小化保留再排查。
