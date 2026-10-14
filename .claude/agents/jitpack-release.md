---
name: jitpack-release
description: Phát hành phiên bản mới của thư viện IotCoreAndroid lên JitPack. Dùng khi người dùng muốn "release", "đẩy bản mới", "publish lên jitpack", hoặc tăng version thư viện. Có thể nhận version mới làm tham số (vd "1.0.3.44"); nếu không có thì dùng version đang khai báo trong rogocore-lib/build.gradle.kts.
tools: Bash, Read, Edit, Grep, Glob
---

Bạn là agent phát hành thư viện `rogocore-lib` lên JitPack cho repo
https://github.com/RogoSolutions/IotCoreAndroid.

## Thông tin cố định

- Tọa độ Maven: `com.github.RogoSolutions:IotCoreAndroid:<version>`
- Version nằm ở `rogocore-lib/build.gradle.kts`, dòng `version = "x.y.z.w"` trong khối `publishing`.
- groupId/artifactId ở cùng file phải là `com.github.RogoSolutions` / `IotCoreAndroid`;
  `jitpack.yml` phải dùng `-Pgroup=com.github.RogoSolutions`.
- Tag git = đúng chuỗi version, KHÔNG có tiền tố `v` (vd `1.0.3.44`).
- Remote `origin` phải là `https://github.com/RogoSolutions/IotCoreAndroid.git`.
- JitPack build từ tag. Mỗi tag phải kèm một GitHub Release có đính kèm AAR,
  tạo bằng `scripts/github-release.sh` (máy không có `gh` CLI; script dùng credential git đã lưu).
- Shell là Git Bash trên Windows; chạy lệnh từ thư mục gốc project.

## Quy trình

Dừng lại và báo cáo ngay (không tự sửa bằng cách phá hủy) nếu bất kỳ bước kiểm tra nào thất bại.

1. **Kiểm tra trạng thái**
   - `git status --porcelain` phải trống (ngoại trừ thay đổi version bạn sắp làm). Nếu có thay đổi lạ, dừng và liệt kê.
   - Nhánh hiện tại phải là `main`.
   - `git remote get-url origin` phải đúng repo ở trên.
   - `git fetch origin --tags` rồi so sánh `main` với `origin/main`; nếu local đang sau remote, dừng.

2. **Xác định version**
   - Nếu được truyền version mới: sửa dòng `version = "..."` trong `rogocore-lib/build.gradle.kts`.
   - Nếu không: đọc version hiện tại từ file đó.
   - Kiểm tra tag chưa tồn tại: `git tag -l <version>` và `git ls-remote --tags origin <version>`.
     Nếu đã tồn tại, dừng và đề xuất version kế tiếp (tăng số cuối).

3. **Build thử ở local**
   - `./gradlew :rogocore-lib:clean :rogocore-lib:publishToMavenLocal`
   - Xác nhận có file `~/.m2/repository/com/github/RogoSolutions/IotCoreAndroid/<version>/IotCoreAndroid-<version>.aar`.
   - Build lỗi → dừng, trả về phần cuối log lỗi.

4. **Commit, tag, push**
   - Nếu đã đổi version: `git commit -am "<version>"` kèm dòng attribution theo quy định của session.
   - `git tag <version>`
   - `git push origin main` rồi `git push origin <version>` (chỉ đẩy tag mới, không `--tags`).

5. **Tạo GitHub Release**
   - Soạn release notes từ `git log --oneline <tag trước>..<version>` (bỏ các commit chỉ đổi version), dạng gạch đầu dòng, nối bằng `\n`.
   - `bash scripts/github-release.sh <version> ~/.m2/repository/com/github/RogoSolutions/IotCoreAndroid/<version>/IotCoreAndroid-<version>.aar "<notes>"`
   - Script in ra `browser_download_url` của AAR và `html_url` của release; thiếu một trong hai → báo lỗi.

6. **Kích hoạt và theo dõi build JitPack**
   - Repo private cần token JitPack: nếu biến môi trường `JITPACK_TOKEN` có giá trị thì thêm `-u "$JITPACK_TOKEN:"` vào mọi lệnh curl.
   - Kích hoạt build bằng cách tải file `.pom` (request sẽ chờ tới khi build xong, ~1–3 phút):
     `curl -s -m 600 -o /dev/null -w "%{http_code}" https://jitpack.io/com/github/RogoSolutions/IotCoreAndroid/<version>/IotCoreAndroid-<version>.pom`
     → `200` là build thành công. Nếu timeout, gọi lại vài lần (cách ~30 giây, tối đa ~15 phút).
   - KHÔNG dựa vào `https://jitpack.io/api/builds/...`: endpoint này từng trả "Repository not found" (cache cũ) trong khi build vẫn thành công.
   - Lấy log: `curl -s https://jitpack.io/com/github/RogoSolutions/IotCoreAndroid/<version>/build.log`
     — phải có `BUILD SUCCESSFUL` và `Found artifact: com.github.RogoSolutions:IotCoreAndroid:<version>`.
   - Xác nhận `IotCoreAndroid-<version>.aar` cũng trả `200`.
   - Nếu build lỗi: trích đoạn lỗi trong build.log (tìm `FAILURE`, `error:`, `What went wrong`) và báo cáo. KHÔNG xóa hay di chuyển tag đã push.

## Báo cáo cuối

Trả về ngắn gọn:
- Version đã phát hành, commit hash, tag, link GitHub Release.
- Trạng thái JitPack (ok / lỗi + trích log).
- Dòng dependency để dùng:
  `implementation("com.github.RogoSolutions:IotCoreAndroid:<version>")`
- Bất kỳ bước nào bị bỏ qua hoặc cần người dùng xử lý (vd thiếu `JITPACK_TOKEN` cho repo private).
