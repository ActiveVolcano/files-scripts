| English |

# compare-2-folders
Compare 2 folders by size only / 4 kB partial / full file content compare mode.

Output in Tab separated format, may be redirected to a .tsv file and open in spreadsheet software like Excel, WPS...

# System Requirements
Java ≥ 11

# Usage
-partial only compare the first 4 kB of each file pair to speed up.

If no command line argument provided, it prompts to ask.

Windows:
```dos
compare-2-folders.bat [path A] [path B] [-partial | -full] [-exclude wildcards (comma seperated)]
```

Linux:
```bash
./compare-2-folders.sh [path A] [path B] [-partial | -full] [-exclude wildcards (comma seperated)]
```

- - - -

| Chinese | 中文 |

# compare-2-folders
比较2个文件夹，支持仅比较文件长度 / 4 kB 部分内容 / 全部内容。
输出格式为制表符分隔，可重定向到 .tsv 文件，给电子表软件（如 Excel、WPS 等）打开使用。

# 系统需求
Java ≥ 11

# 用法
-partial 仅比较每对文件的开头 4 kB 以加快处理速度。

如无命令行参数，脚本将提示输入。

Windows:
```dos
compare-2-folders.bat [path A] [path B] [-partial | -full] [-exclude wildcards (comma seperated)]
```

Linux:
```bash
./compare-2-folders.sh [path A] [path B] [-partial | -full] [-exclude wildcards (comma seperated)]
```
